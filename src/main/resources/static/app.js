let board = null;
let game = new Chess();
let ws = null;
let reconnectInterval = null;

// Initialize the chess board
function initBoard() {
    const config = {
        draggable: false,
        position: 'start',
        pieceTheme: 'https://chessboardjs.com/img/chesspieces/wikipedia/{piece}.png'
    };
    board = Chessboard('board', config);
}

// Connect to WebSocket
function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/chess`;

    console.log('Connecting to WebSocket:', wsUrl);
    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket connected');
        updateConnectionStatus(true);
        if (reconnectInterval) {
            clearInterval(reconnectInterval);
            reconnectInterval = null;
        }
    };

    ws.onmessage = function(event) {
        console.log('WebSocket message:', event.data);
        try {
            const data = JSON.parse(event.data);
            handleGameUpdate(data);
        } catch (e) {
            console.error('Error parsing WebSocket message:', e);
        }
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
        updateConnectionStatus(false);
    };

    ws.onclose = function() {
        console.log('WebSocket disconnected');
        updateConnectionStatus(false);

        // Try to reconnect
        if (!reconnectInterval) {
            reconnectInterval = setInterval(function() {
                console.log('Attempting to reconnect...');
                connectWebSocket();
            }, 5000);
        }
    };
}

// Update connection status indicator
function updateConnectionStatus(connected) {
    const statusIndicator = document.getElementById('wsStatus');
    const statusText = document.getElementById('wsStatusText');

    if (connected) {
        statusIndicator.className = 'status-indicator connected';
        statusText.textContent = 'Connected';
    } else {
        statusIndicator.className = 'status-indicator disconnected';
        statusText.textContent = 'Disconnected - Reconnecting...';
    }
}

// Handle game updates from WebSocket
function handleGameUpdate(data) {
    if (data.type === 'game_update') {
        // Update game state
        game.load(data.fen);
        board.position(data.fen);

        // Update player info
        document.getElementById('playerName').textContent = data.playerName || '-';
        document.getElementById('playerSide').textContent = data.playerSide || '-';
        document.getElementById('currentTurn').textContent = data.currentTurn || '-';

        // Update status
        const statusDiv = document.getElementById('gameStatus');
        if (data.gameOver) {
            statusDiv.innerHTML = `
                <div class="game-over">
                    <strong>Game Over!</strong><br>
                    ${data.result || 'Game ended'}
                </div>
            `;
        } else {
            statusDiv.innerHTML = `
                <div class="game-active">
                    <strong>Game in Progress</strong><br>
                    ${data.playerName} (${data.playerSide}) vs Stockfish<br>
                    Current turn: ${data.currentTurn}
                </div>
            `;
        }

        // Update move history
        updateMoveHistory(data.moveHistory);
    }
}

// Update move history display
function updateMoveHistory(moves) {
    const moveHistoryDiv = document.getElementById('moveHistory');

    if (!moves || moves.length === 0) {
        moveHistoryDiv.innerHTML = '<p class="empty">No moves yet</p>';
        return;
    }

    let html = '';
    for (let i = 0; i < moves.length; i += 2) {
        const moveNum = Math.floor(i / 2) + 1;
        const whiteMove = moves[i];
        const blackMove = moves[i + 1] || '';
        html += `<div class="move-item">${moveNum}. ${whiteMove} ${blackMove}</div>`;
    }

    moveHistoryDiv.innerHTML = html;

    // Auto-scroll to bottom
    moveHistoryDiv.scrollTop = moveHistoryDiv.scrollHeight;
}

// Flip board
document.getElementById('flipBoard').addEventListener('click', function() {
    board.flip();
});

// Load active games on startup
async function loadActiveGames() {
    try {
        const response = await fetch('/api/games');
        const games = await response.json();

        // If there are active games, display the first one
        const gameIds = Object.keys(games);
        if (gameIds.length > 0) {
            const firstGame = games[gameIds[0]];
            handleGameUpdate({
                type: 'game_update',
                gameId: firstGame.gameId,
                playerName: firstGame.playerName,
                playerSide: firstGame.playerSide,
                fen: firstGame.fen,
                moveHistory: firstGame.moveHistory,
                gameOver: firstGame.gameOver,
                result: firstGame.result,
                currentTurn: firstGame.board.sideToMove
            });
        }
    } catch (e) {
        console.error('Error loading active games:', e);
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initBoard();
    connectWebSocket();
    loadActiveGames();
});
