package client

import ai.SmartAIPlayer
import com.google.gson.Gson
import com.google.gson.JsonObject
import core.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture

class TournamentClient(
    private val serverUrl: String,
    private val playerName: String,
) {
    private val httpClient = HttpClient.newHttpClient()
    private val gson = Gson()
    private val done = CompletableFuture<Unit>()

    // AI & game state
    private val ai = SmartAIPlayer()
    private var currentGameId: String? = null
    private var pendingPlacements = mutableListOf<ShipPlacement>()
    private var placementIndex = 0

    private var ws: WebSocket? = null

    fun run() {
        val clientId = register()
        println("[CLIENT] Zarejestrowano jako: $clientId")
        connectWebSocket(clientId)
        done.get() // blokuje do TOURNAMENT_END lub zamknięcia WS
        println("[CLIENT] Sesja zakończona.")
    }

    // ─── HTTP Registration ────────────────────────────────────────────────────

    private fun register(): String {
        val body = """{"name":"$playerName"}"""
        val req = HttpRequest.newBuilder()
            .uri(URI("$serverUrl/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        val json = gson.fromJson(resp.body(), JsonObject::class.java)
        return when (resp.statusCode()) {
            200 -> json["clientId"].asString
            409 -> {
                println("[CLIENT] Nazwa '$playerName' już zajęta — próba ponownego połączenia...")
                playerName
            }
            else -> throw RuntimeException("Rejestracja nieudana: ${resp.statusCode()} ${resp.body()}")
        }
    }

    // ─── WebSocket ────────────────────────────────────────────────────────────

    private fun connectWebSocket(clientId: String) {
        val wsBase = serverUrl.replace("http://", "ws://").replace("https://", "wss://")
        val uri = URI("$wsBase/api/client/ws?clientId=$clientId")
        println("[WS] Łączenie z $uri")
        ws = httpClient.newWebSocketBuilder()
            .buildAsync(uri, Listener())
            .join()
    }

    private fun send(message: String) {
        println("[>>] $message")
        ws?.sendText(message, true)
    }

    private fun sendPlacement(gameId: String, p: ShipPlacement) {
        val dir = p.direction.name // "HORIZONTAL" or "VERTICAL"
        send(
            """{"type":"move","move":{"gameId":"$gameId","type":"SHIP_PLACEMENT","data":{"size":${p.size},"position":{"x":${p.position.x},"y":${p.position.y}},"direction":"$dir"}}}""",
        )
    }

    private fun sendShot(gameId: String, c: Coordinate) {
        send(
            """{"type":"move","move":{"gameId":"$gameId","type":"SHOT","data":{"position":{"x":${c.x},"y":${c.y}}}}}""",
        )
    }

    // ─── Message Dispatch ─────────────────────────────────────────────────────

    private fun handleMessage(text: String) {
        println("[<<] $text")
        val root = try {
            gson.fromJson(text, JsonObject::class.java)
        } catch (e: Exception) {
            println("[ERROR] Nie można sparsować JSON: $text"); return
        }
        when (root["type"]?.asString) {
            "connected" -> println("[CLIENT] Połączono z serwerem (id=${root["clientId"]?.asString})")
            "event" -> {
                val ev = root["event"]?.asJsonObject ?: return
                val evType = ev["eventType"]?.asString ?: return
                val data = ev["data"]?.asJsonObject ?: JsonObject()
                handleEvent(evType, data)
            }
            "error" -> println("[ERROR] ${root["error"]?.asString}: ${root["message"]?.asString}")
        }
    }

    // ─── Event Handlers ───────────────────────────────────────────────────────

    private fun handleEvent(type: String, d: JsonObject) {
        when (type) {
            "CONNECTED_WAIT_FOR_START" -> {
                val c = d["connectedPlayers"]?.asInt ?: 0
                val t = d["totalPlayers"]?.asInt ?: 0
                println("[EVENT] Oczekiwanie na graczy: $c/$t")
            }

            "TOURNAMENT_START" -> {
                val players = d["totalPlayers"]?.asInt ?: 0
                val games = d["totalGames"]?.asInt ?: 0
                println("[EVENT] *** Turniej start! $players graczy, $games gier ***")
            }

            "GAME_SETUP" -> {
                val gameId = d["gameId"]?.asString ?: return
                val opponent = d["opponentId"]?.asString ?: "?"
                println("\n[GAME] == Nowa gra: $gameId vs $opponent ==")

                currentGameId = gameId
                ai.reset()
                pendingPlacements = ai.placeShips().toMutableList()
                placementIndex = 0
                sendPlacement(gameId, pendingPlacements[placementIndex])
            }

            "SHIP_PLACEMENT_RESPONSE" -> {
                val gameId = d["gameId"]?.asString ?: return
                when (d["status"]?.asString) {
                    "ACCEPTED" -> {
                        val rem = d["shipsRemaining"]?.asInt ?: 0
                        println("[PLACE] ✓ Statki pozostałe: $rem")
                        if (rem > 0) {
                            placementIndex++
                            if (placementIndex < pendingPlacements.size) {
                                sendPlacement(gameId, pendingPlacements[placementIndex])
                            }
                        }
                    }
                    "REJECTED" -> {
                        val err = d["error"]?.asString ?: "UNKNOWN"
                        println("[PLACE] ✗ Odrzucono ($err) — regeneruję miejsce...")
                        val failedSize = pendingPlacements[placementIndex].size
                        val alt = regeneratePlacement(failedSize)
                        if (alt != null) {
                            pendingPlacements[placementIndex] = alt
                            sendPlacement(gameId, alt)
                        } else {
                            println("[ERROR] Brak miejsca dla statku rozmiaru $failedSize!")
                        }
                    }
                }
            }

            "GAME_START" -> {
                val gameId = d["gameId"]?.asString ?: return
                val myTurn = d["yourTurn"]?.asBoolean ?: false
                println("[GAME] Start strzelania! Moja tura: $myTurn")
                if (myTurn) fireShot(gameId)
            }

            "SHOT_ACK" -> {
                val gameId = d["gameId"]?.asString ?: return
                val x = d["position"]?.asJsonObject?.get("x")?.asInt ?: return
                val y = d["position"]?.asJsonObject?.get("y")?.asInt ?: return
                val result = d["result"]?.asString ?: return
                val myTurn = d["yourTurn"]?.asBoolean ?: false

                when (result) {
                    "MISS" -> { ai.onShotResult(Coordinate(x, y), ShotResult.Miss); println("[SHOT] ($x,$y) → pudło") }
                    "HIT" -> { ai.onShotResult(Coordinate(x, y), ShotResult.Hit); println("[SHOT] ($x,$y) → TRAFIENIE!") }
                    "SUNK" -> {
                        val sz = d["sunkShip"]?.asJsonObject?.get("size")?.asInt ?: 2
                        ai.onShotResult(Coordinate(x, y), ShotResult.Sunk(sz))
                        println("[SHOT] ($x,$y) → ZATOPIONY statek ($sz)!")
                    }
                    "INVALID" -> println("[SHOT] ($x,$y) → nieprawidłowy: ${d["error"]?.asString}")
                }

                if (myTurn) fireShot(gameId)
            }

            "ENEMY_SHOT" -> {
                val gameId = d["gameId"]?.asString ?: return
                val x = d["position"]?.asJsonObject?.get("x")?.asInt ?: return
                val y = d["position"]?.asJsonObject?.get("y")?.asInt ?: return
                val result = d["result"]?.asString ?: return
                val myTurn = d["yourTurn"]?.asBoolean ?: false
                println("[ENEMY] Strzał w ($x,$y): $result | moja tura: $myTurn")
                if (myTurn) fireShot(gameId)
            }

            "GAME_END" -> {
                val gameId = d["gameId"]?.asString ?: return
                val result = d["result"]?.asString ?: return
                val myShots = d["yourTotalShots"]?.asInt ?: 0
                val enemyShots = d["enemyTotalShots"]?.asInt ?: 0
                println("[GAME] == $gameId zakończona: $result (moje: $myShots, wroga: $enemyShots) ==\n")
                currentGameId = null
            }

            "TOURNAMENT_END" -> {
                println("\n========================================")
                println("          KONIEC TURNIEJU")
                println("========================================")
                d["standings"]?.asJsonArray?.forEach { item ->
                    val s = item.asJsonObject
                    val rank = s["rank"]?.asInt ?: 0
                    val id = s["clientId"]?.asString ?: "?"
                    val wins = s["wins"]?.asInt ?: 0
                    val losses = s["losses"]?.asInt ?: 0
                    val disqs = s["disqualifications"]?.asInt ?: 0
                    val rate = ((s["winRate"]?.asDouble ?: 0.0) * 100).toInt()
                    println(" $rank. ${id.padEnd(22)} W:$wins L:$losses DQ:$disqs ($rate%)")
                }
                println("========================================\n")
                done.complete(Unit)
            }
        }
    }

    // ─── AI Actions ───────────────────────────────────────────────────────────

    private fun fireShot(gameId: String) {
        val coord = ai.getNextShot()
        println("[SHOT] Strzelam w (${coord.x},${coord.y})")
        sendShot(gameId, coord)
    }

    /**
     * Generates a valid placement for [size] that doesn't conflict with
     * the ships already accepted (indices 0 until placementIndex).
     */
    private fun regeneratePlacement(size: Int): ShipPlacement? {
        val placed = pendingPlacements.take(placementIndex).map { it.toShip() }
        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                for (dir in Direction.entries) {
                    val candidate = ShipPlacement(size, Coordinate(x, y), dir)
                    val ship = candidate.toShip()
                    if (ship.isValid() && GameRules.canPlaceShip(ship, placed)) return candidate
                }
            }
        }
        return null
    }

    // ─── WebSocket Listener ───────────────────────────────────────────────────

    inner class Listener : WebSocket.Listener {
        private val buf = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            println("[WS] Połączenie otwarte")
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletableFuture<*>? {
            buf.append(data)
            if (last) {
                val msg = buf.toString()
                buf.clear()
                handleMessage(msg)
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletableFuture<*>? {
            println("[WS] Zamknięto: $statusCode — $reason")
            if (!done.isDone) done.complete(Unit)
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            println("[WS] Błąd: ${error.message}")
            if (!done.isDone) done.completeExceptionally(error)
        }
    }
}
