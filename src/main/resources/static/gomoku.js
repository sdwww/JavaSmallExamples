/**
 * 五子棋游戏逻辑
 * 版本: 20260405-v12
 */

// ==================== 全局变量 ====================
const BOARD_SIZE = 15;
let sessionId = null;
let board = [];
let currentPlayer = 1;
let gameOver = false;
let lastMove = null;
let isThinking = false;
let moveCountNum = 0;
let aiThinkStartTime = 0;
let soundEnabled = true;
let audioContext = null;
let moveOrder = [];

// ==================== 音频相关 ====================

function initAudio() {
    if (!audioContext) {
        try {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            console.log('浏览器不支持音频API');
        }
    }
}

function playMoveSound() {
    if (!soundEnabled || !audioContext) return;
    
    try {
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.frequency.setValueAtTime(800, audioContext.currentTime);
        oscillator.frequency.exponentialRampToValueAtTime(400, audioContext.currentTime + 0.1);
        
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1);
        
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.1);
    } catch (e) {
        console.error('播放音效失败:', e);
    }
}

function playWinSound() {
    if (!soundEnabled || !audioContext) return;
    
    try {
        const notes = [523, 659, 784, 1047];
        notes.forEach((freq, i) => {
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);
            
            oscillator.frequency.setValueAtTime(freq, audioContext.currentTime + i * 0.15);
            gainNode.gain.setValueAtTime(0.3, audioContext.currentTime + i * 0.15);
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + i * 0.15 + 0.3);
            
            oscillator.start(audioContext.currentTime + i * 0.15);
            oscillator.stop(audioContext.currentTime + i * 0.15 + 0.3);
        });
    } catch (e) {
        console.error('播放胜利音效失败:', e);
    }
}

function toggleSound() {
    soundEnabled = !soundEnabled;
    const btn = document.querySelector('.sound-toggle');
    btn.textContent = soundEnabled ? '🔊' : '🔇';
    if (soundEnabled) {
        initAudio();
    }
}

// ==================== UI更新函数 ====================

function updateMoveCount(count) {
    moveCountNum = count;
    document.getElementById('moveCount').textContent = count;
}

function updateThinkTime(ms) {
    if (ms !== null && ms !== undefined) {
        document.getElementById('thinkTime').textContent = (ms / 1000).toFixed(1) + '秒';
    } else {
        document.getElementById('thinkTime').textContent = '-';
    }
}

function updateTurnIndicator(player) {
    const turnPiece = document.getElementById('turnPiece');
    const currentPlayerText = document.getElementById('currentPlayerText');
    
    if (player === 1) {
        turnPiece.className = 'turn-piece black';
        currentPlayerText.textContent = '你(黑方)';
    } else {
        turnPiece.className = 'turn-piece white';
        currentPlayerText.textContent = 'AI(白方)';
    }
}

function updateCurrentPlayer() {
    updateTurnIndicator(currentPlayer);
}

function updateUndoButton(canUndo) {
    const btn = document.getElementById('undoBtn');
    if (btn) {
        btn.disabled = !canUndo || gameOver;
    }
}

function updateHistory(historyList) {
    const listEl = document.getElementById('historyList');
    if (!listEl) return;
    
    if (!historyList || historyList.length === 0) {
        listEl.innerHTML = '<div style="color:rgba(255,255,255,0.5);text-align:center;padding:20px 0;">暂无落子</div>';
        moveOrder = [];
        return;
    }
    
    moveOrder = [];
    for (let text of historyList) {
        const match = text.match(/\d+\.\s*(黑|白)\s*([A-O])(\d+)/);
        if (match) {
            const player = match[1] === '黑' ? 1 : 2;
            const col = match[2].charCodeAt(0) - 65;
            const row = parseInt(match[3]) - 1;
            moveOrder.push({ row, col, player });
        }
    }
    
    listEl.innerHTML = '';
    historyList.forEach((text, idx) => {
        const div = document.createElement('div');
        div.className = 'move-item' + (idx === historyList.length - 1 ? ' latest' : '');
        div.textContent = text;
        listEl.appendChild(div);
    });
    
    listEl.scrollTop = listEl.scrollHeight;
}

function showVictory(winnerText) {
    const overlay = document.getElementById('victoryOverlay');
    const text = document.getElementById('victoryText');
    text.textContent = winnerText;
    overlay.classList.add('show');
}

function closeVictoryAndReset() {
    const overlay = document.getElementById('victoryOverlay');
    overlay.classList.remove('show');
    resetGame();
}

function updateVictoryDisplay(winnerText) {
    if (winnerText) {
        document.getElementById('message').textContent = winnerText;
        document.getElementById('message').className = 'game-over';
        playWinSound();
        setTimeout(() => showVictory(winnerText), 500);
    }
}

// ==================== 棋盘初始化 ====================

function initBoard() {
    const boardRowsEl = document.getElementById('boardRows');
    const colLabelsEl = document.getElementById('colLabels');
    boardRowsEl.innerHTML = '';
    colLabelsEl.innerHTML = '';

    for (let j = 0; j < BOARD_SIZE; j++) {
        const label = document.createElement('span');
        label.textContent = String.fromCharCode(65 + j);
        colLabelsEl.appendChild(label);
    }

    const starPoints = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]];

    for (let i = 0; i < BOARD_SIZE; i++) {
        const rowWrapper = document.createElement('div');
        rowWrapper.className = 'board-row-wrapper';
        
        const rowLabel = document.createElement('span');
        rowLabel.className = 'row-label';
        rowLabel.textContent = (i + 1).toString();
        rowWrapper.appendChild(rowLabel);
        
        for (let j = 0; j < BOARD_SIZE; j++) {
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.dataset.row = i;
            cell.dataset.col = j;
            
            if (starPoints.some(p => p[0] === i && p[1] === j)) {
                cell.classList.add('star-point');
            }
            
            cell.onclick = () => makeMove(i, j);
            rowWrapper.appendChild(cell);
        }
        boardRowsEl.appendChild(rowWrapper);
    }
    
    updateMoveCount(0);
    updateThinkTime(null);
    updateTurnIndicator(1);
    document.getElementById('message').textContent = '';
    document.getElementById('message').className = '';
    updateUndoButton(false);
    updateHistory([]);
    moveOrder = [];
    
    if ('ontouchstart' in window) {
        initGestures();
    }
}

function renderBoard() {
    const boardRowsEl = document.getElementById('boardRows');
    if (!boardRowsEl || boardRowsEl.children.length === 0) {
        console.warn('棋盘尚未初始化，跳过渲染');
        return;
    }
    
    const starPoints = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]];
    
    for (let i = 0; i < BOARD_SIZE; i++) {
        const rowWrapper = boardRowsEl.children[i];
        if (!rowWrapper) continue;
        
        for (let j = 0; j < BOARD_SIZE; j++) {
            const cell = rowWrapper.children[j + 1];
            if (!cell) continue;
            
            cell.innerHTML = '';
            cell.className = 'cell';
            
            if (starPoints.some(p => p[0] === i && p[1] === j)) {
                cell.classList.add('star-point');
            }
            
            if (currentPlayer === 2 && !gameOver) {
                cell.classList.add('disabled');
            }

            if (board[i][j] !== 0) {
                cell.classList.add('occupied');
                cell.classList.remove('disabled');
                const piece = document.createElement('div');
                piece.className = 'piece ' + (board[i][j] === 1 ? 'black' : 'white');
                cell.appendChild(piece);
            }
        }
    }
}

function highlightMove(row, col, animate = true) {
    const boardRowsEl = document.getElementById('boardRows');
    if (!boardRowsEl || !boardRowsEl.children[row]) return;
    const cell = boardRowsEl.children[row].children[col + 1];
    if (!cell) return;
    const piece = cell.querySelector('.piece');
    if (piece) {
        piece.classList.add('last');
        if (animate) {
            piece.classList.add('animate');
        }
    }
    lastMove = { row, col };
}

// ==================== 游戏控制 ====================

function getDifficulty() {
    return parseInt(document.getElementById('difficulty').value);
}

async function applyDifficulty() {
    const difficulty = getDifficulty();
    const firstPlayer = parseInt(document.getElementById('firstPlayer').value);
    try {
        await fetch(`/api/gomoku/reset/${sessionId}?difficulty=${difficulty}&firstPlayer=${firstPlayer}`, {
            method: 'POST'
        });
        initGame();
    } catch (error) {
        console.error('设置失败:', error);
        initGame();
    }
}

async function initGame() {
    try {
        console.log('开始初始化游戏...');
        initAudio();
        initBoard();
        console.log('棋盘DOM已初始化');
        
        board = Array(BOARD_SIZE).fill(null).map(() => Array(BOARD_SIZE).fill(0));
        
        const difficulty = getDifficulty();
        const firstPlayer = parseInt(document.getElementById('firstPlayer').value);
        const response = await fetch(`/api/gomoku/new?difficulty=${difficulty}&firstPlayer=${firstPlayer}`);
        const data = await response.json();
        
        console.log('新游戏响应:', data);
        
        sessionId = data.sessionId;
        board = data.board;
        currentPlayer = data.currentPlayer;
        gameOver = data.gameOver;
        updateMoveCount(data.moveCount || 0);
        updateThinkTime(null);
        updateHistory(data.moveHistory || []);
        updateUndoButton(data.canUndo || false);
        
        renderBoard();
        updateCurrentPlayer();
        document.getElementById('message').textContent = '';
        document.getElementById('message').className = '';
        
        if (currentPlayer === 2 && !gameOver) {
            isThinking = true;
            aiThinkStartTime = Date.now();
            document.getElementById('message').textContent = 'AI思考中...';
            document.getElementById('message').className = 'thinking';
            await pollAiMove();
        }
        
        console.log('游戏初始化完成');
    } catch (error) {
        console.error('初始化游戏失败:', error);
        initBoard();
        board = Array(BOARD_SIZE).fill(null).map(() => Array(BOARD_SIZE).fill(0));
        renderBoard();
    }
}

async function resetGame() {
    if (isThinking) {
        console.log('正在思考中，请稍候...');
        return;
    }
    
    isThinking = false;
    gameOver = false;
    lastMove = null;
    
    try {
        console.log('开始重置游戏...');
        initBoard();
        console.log('棋盘DOM已重置');
        
        const difficulty = getDifficulty();
        const firstPlayer = parseInt(document.getElementById('firstPlayer').value);
        const response = await fetch(`/api/gomoku/new?difficulty=${difficulty}&firstPlayer=${firstPlayer}`);
        const data = await response.json();
        
        console.log('新游戏响应:', data);

        sessionId = data.sessionId;
        board = data.board;
        currentPlayer = data.currentPlayer;
        gameOver = data.gameOver;
        
        updateMoveCount(data.moveCount || 0);
        updateThinkTime(null);
        updateHistory(data.moveHistory || []);
        updateUndoButton(data.canUndo || false);

        document.getElementById('message').textContent = '';
        document.getElementById('message').className = '';

        renderBoard();
        updateCurrentPlayer();
        
        console.log('游戏重置完成 - 棋盘:', board, '当前玩家:', currentPlayer, '游戏结束:', gameOver);
        
        if (currentPlayer === 2 && !gameOver) {
            isThinking = true;
            aiThinkStartTime = Date.now();
            document.getElementById('message').textContent = 'AI思考中...';
            document.getElementById('message').className = 'thinking';
            await pollAiMove();
        }
        
        console.log('页面显示 - 当前玩家:', document.getElementById('currentPlayer').textContent, '步数:', document.getElementById('moveCount').textContent);
    } catch (error) {
        console.error('重置游戏失败:', error);
        initGame();
    }
}

// ==================== 落子逻辑 ====================

async function makeMove(row, col) {
    if (gameOver || board[row][col] !== 0 || currentPlayer !== 1 || isThinking) return;

    try {
        isThinking = true;
        aiThinkStartTime = Date.now();
        document.getElementById('message').textContent = 'AI思考中...';
        document.getElementById('message').className = 'thinking';
        
        playMoveSound();

        const response = await fetch(`/api/gomoku/move/${sessionId}?row=${row}&col=${col}`, {
            method: 'POST'
        });
        const data = await response.json();

        if (data.playerMoveSuccess) {
            board = data.board;
            gameOver = data.gameOver;
            currentPlayer = data.currentPlayer;
            updateMoveCount(data.moveCount || 0);
            updateHistory(data.moveHistory || []);
            updateUndoButton(data.canUndo);
            
            renderBoard();
            
            if (data.playerMove) {
                highlightMove(data.playerMove[0], data.playerMove[1]);
            }
            
            if (data.aiNeedPoll) {
                await pollAiMove();
            } else if (data.aiMove) {
                highlightMove(data.aiMove[0], data.aiMove[1]);
                if (data.gameOver && data.winnerText) {
                    gameOver = data.gameOver;
                    updateMoveCount(data.moveCount || 0);
                    updateVictoryDisplay(data.winnerText);
                    updateUndoButton(data.canUndo);
                } else {
                    updateCurrentPlayer();
                    updateMoveCount(data.moveCount || 0);
                    document.getElementById('message').textContent = '';
                    document.getElementById('message').className = '';
                }
                isThinking = false;
            } else {
                if (gameOver && data.winnerText) {
                    updateMoveCount(data.moveCount || 0);
                    updateVictoryDisplay(data.winnerText);
                    updateUndoButton(data.canUndo);
                } else {
                    updateCurrentPlayer();
                    updateMoveCount(data.moveCount || 0);
                    document.getElementById('message').textContent = '';
                    document.getElementById('message').className = '';
                }
                isThinking = false;
            }
        } else if (data.error) {
            document.getElementById('message').textContent = data.error;
            document.getElementById('message').className = '';
            isThinking = false;
        }
    } catch (error) {
        console.error('落子失败:', error);
        isThinking = false;
    }
}

async function pollAiMove() {
    let retryCount = 0;
    const maxRetries = 300;
    
    while (isThinking && retryCount < maxRetries) {
        try {
            const response = await fetch(`/api/gomoku/ai-move/${sessionId}`);
            const data = await response.json();

            if (data.error) {
                console.error('AI落子错误:', data.error);
                if (data.error === '游戏不存在') {
                    isThinking = false;
                    initGame();
                    return;
                }
            }

            if (data.aiMove) {
                if (data.board) board = data.board;
                if (data.currentPlayer !== undefined) currentPlayer = data.currentPlayer;
                if (data.gameOver !== undefined) gameOver = data.gameOver;
                
                renderBoard();
                highlightMove(data.aiMove[0], data.aiMove[1]);
                updateHistory(data.moveHistory || []);
                updateUndoButton(data.canUndo);
                
                if (gameOver) {
                    updateVictoryDisplay(data.winnerText || '游戏结束');
                } else {
                    updateCurrentPlayer();
                    document.getElementById('message').textContent = '';
                    document.getElementById('message').className = '';
                }
                
                if (aiThinkStartTime) {
                    updateThinkTime((Date.now() - aiThinkStartTime) / 1000);
                }
                
                isThinking = false;
                break;
            }
            
            if (data.aiThinking && aiThinkStartTime) {
                const elapsed = ((Date.now() - aiThinkStartTime) / 1000).toFixed(1);
                document.getElementById('message').textContent = `AI思考中... ${elapsed}s`;
                document.getElementById('message').className = 'thinking';
            }
            
            retryCount++;
        } catch (error) {
            console.error('获取AI落子失败:', error);
            retryCount++;
        }

        await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    if (retryCount >= maxRetries) {
        console.error('AI落子超时');
        document.getElementById('message').textContent = 'AI响应超时，请重试';
        document.getElementById('message').className = '';
        isThinking = false;
    }
}

// ==================== 悔棋功能 ====================

async function undoMove() {
    if (!sessionId || isThinking || gameOver) return;
    
    try {
        const response = await fetch(`/api/gomoku/undo/${sessionId}`, { method: 'POST' });
        const data = await response.json();
        
        if (data.undoSuccess) {
            board = data.board;
            currentPlayer = data.currentPlayer;
            gameOver = data.gameOver;
            lastMove = null;
            updateMoveCount(data.moveCount || 0);
            updateHistory(data.moveHistory || []);
            updateUndoButton(data.canUndo);
            renderBoard();
            updateCurrentPlayer();
            document.getElementById('message').textContent = '';
            document.getElementById('message').className = '';
        } else {
            document.getElementById('message').textContent = data.error || '无法悔棋';
            setTimeout(() => {
                document.getElementById('message').textContent = '';
            }, 2000);
        }
    } catch (error) {
        console.error('悔棋失败:', error);
    }
}

// ==================== 导出功能 ====================

function exportBoardImage() {
    if (!board || board.length === 0) {
        alert('当前没有棋盘数据');
        return;
    }
    
    try {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        
        const cellSize = 40;
        const margin = 30;
        const labelWidth = 25;
        const width = margin + labelWidth + BOARD_SIZE * cellSize + margin;
        const height = margin + labelWidth + BOARD_SIZE * cellSize + margin;
        canvas.width = width;
        canvas.height = height;
        
        ctx.fillStyle = '#DEB887';
        ctx.fillRect(0, 0, width, height);
        
        ctx.fillStyle = '#333';
        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('五子棋棋谱', width / 2, 20);
        
        ctx.strokeStyle = '#8B7355';
        ctx.lineWidth = 1;
        for (let i = 0; i < BOARD_SIZE; i++) {
            const y = margin + labelWidth + i * cellSize + cellSize / 2;
            ctx.beginPath();
            ctx.moveTo(margin + labelWidth, y);
            ctx.lineTo(margin + labelWidth + (BOARD_SIZE - 1) * cellSize, y);
            ctx.stroke();
            
            const x = margin + labelWidth + i * cellSize + cellSize / 2;
            ctx.beginPath();
            ctx.moveTo(x, margin + labelWidth);
            ctx.lineTo(x, margin + labelWidth + (BOARD_SIZE - 1) * cellSize);
            ctx.stroke();
        }
        
        ctx.fillStyle = '#5a4a3a';
        ctx.font = 'bold 11px Arial';
        ctx.textAlign = 'center';
        for (let j = 0; j < BOARD_SIZE; j++) {
            const x = margin + labelWidth + j * cellSize + cellSize / 2;
            const label = String.fromCharCode(65 + j);
            ctx.fillText(label, x, margin + labelWidth - 5);
        }
        ctx.textAlign = 'right';
        for (let i = 0; i < BOARD_SIZE; i++) {
            const y = margin + labelWidth + i * cellSize + cellSize / 2 + 4;
            ctx.fillText((i + 1).toString(), margin + labelWidth - 5, y);
        }
        
        const pieceRadius = cellSize * 0.4;
        moveOrder.forEach((move, idx) => {
            const { row, col, player } = move;
            const cx = margin + labelWidth + col * cellSize + cellSize / 2;
            const cy = margin + labelWidth + row * cellSize + cellSize / 2;
            
            ctx.beginPath();
            ctx.arc(cx + 2, cy + 2, pieceRadius, 0, Math.PI * 2);
            ctx.fillStyle = 'rgba(0,0,0,0.3)';
            ctx.fill();
            
            const gradient = ctx.createRadialGradient(
                cx - pieceRadius * 0.3, cy - pieceRadius * 0.3, 0,
                cx, cy, pieceRadius
            );
            if (player === 1) {
                gradient.addColorStop(0, '#555');
                gradient.addColorStop(1, '#000');
            } else {
                gradient.addColorStop(0, '#fff');
                gradient.addColorStop(1, '#ccc');
            }
            ctx.beginPath();
            ctx.arc(cx, cy, pieceRadius, 0, Math.PI * 2);
            ctx.fillStyle = gradient;
            ctx.fill();
            
            ctx.fillStyle = player === 1 ? '#fff' : '#000';
            ctx.font = `bold ${Math.max(9, pieceRadius * 0.6)}px Arial`;
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText((idx + 1).toString(), cx, cy);
        });
        
        const link = document.createElement('a');
        link.download = `gomoku_${Date.now()}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
        
    } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败，请重试');
    }
}

// ==================== 移动端手势支持 ====================

let gesturesInitialized = false;

function initGestures() {
    if (gesturesInitialized) return;
    gesturesInitialized = true;
    
    const container = document.getElementById('boardContainer');
    if (!container) return;
    
    let scale = 1;
    let isDragging = false;
    let startX, startY, translateX = 0, translateY = 0;
    let lastTap = 0;
    
    container.addEventListener('touchend', function(e) {
        const currentTime = new Date().getTime();
        const tapLength = currentTime - lastTap;
        if (tapLength < 300 && tapLength > 0) {
            e.preventDefault();
            if (scale === 1) {
                scale = 1.5;
            } else {
                scale = 1;
                translateX = 0;
                translateY = 0;
            }
            container.style.transform = `translate(${translateX}px, ${translateY}px) scale(${scale})`;
        }
        lastTap = currentTime;
    }, { passive: false });
    
    container.addEventListener('touchstart', function(e) {
        if (scale > 1 && e.touches.length === 1) {
            isDragging = true;
            startX = e.touches[0].clientX - translateX;
            startY = e.touches[0].clientY - translateY;
        }
    }, { passive: true });
    
    container.addEventListener('touchmove', function(e) {
        if (isDragging && e.touches.length === 1) {
            e.preventDefault();
            translateX = e.touches[0].clientX - startX;
            translateY = e.touches[0].clientY - startY;
            container.style.transform = `translate(${translateX}px, ${translateY}px) scale(${scale})`;
        }
    }, { passive: false });
    
    container.addEventListener('touchend', function() {
        isDragging = false;
    }, { passive: true });
}

// ==================== 页面加载时自动初始化 ====================

console.log('五子棋游戏初始化 - 版本 20260405-v12 (UI美化版)');
initGame();
