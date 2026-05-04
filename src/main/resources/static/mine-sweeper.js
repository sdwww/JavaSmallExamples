const API = '/api/mine-sweeper';
let gameId = null, R = 16, C = 16, M = 40;
let sec = 0, timer = null, over = false;

async function init() {
  const difficulty = document.getElementById('difficulty').value;
  const data = await fetch(`${API}/new?difficulty=${difficulty}`).then(r => r.json());
  gameId = data.gameId;
  R = data.rows; C = data.cols; M = data.mines;
  const g = document.getElementById('grid');
  g.style.gridTemplateColumns = `repeat(${C}, 50px)`;
  g.innerHTML = '';
  for (let i = 0; i < R * C; i++) {
    const cell = document.createElement('div');
    cell.className = 'cell';
    const r = Math.floor(i / C), c = i % C;
    cell.onclick = () => click(r, c);
    cell.oncontextmenu = e => { e.preventDefault(); onRightClick(r, c); };
    g.appendChild(cell);
  }
  renderBoard(data.board);
}

function startTimer() {
  if (timer) return;
  timer = setInterval(() => {
    document.getElementById('time').textContent = String(++sec).padStart(3, '0');
  }, 1000);
}

function cell(r, c) {
  return document.getElementById('grid').children[r * C + c];
}

async function click(r, c) {
  if (over) return;
  startTimer();
  const res = await fetch(`${API}/click?gameId=${gameId}&row=${r}&col=${c}`, { method: 'POST' }).then(r => r.json());
  if (res.type === 'mine') {
    over = true; clearInterval(timer);
    document.getElementById('face').textContent = '😵';
    showMsg('💥 游戏结束', 'lose');
  } else if (res.type === 'win') {
    over = true; clearInterval(timer);
    document.getElementById('face').textContent = '😎';
    showMsg('🎉 胜利！', 'win');
  }
  renderBoard(res.board);
}

async function chord(r, c) {
  if (over) return;
  startTimer();
  const res = await fetch(`${API}/chord?gameId=${gameId}&row=${r}&col=${c}`, { method: 'POST' }).then(r => r.json());
  if (res.type === 'mine') {
    over = true; clearInterval(timer);
    document.getElementById('face').textContent = '😵';
    showMsg('💥 游戏结束', 'lose');
  } else if (res.type === 'win') {
    over = true; clearInterval(timer);
    document.getElementById('face').textContent = '😎';
    showMsg('🎉 胜利！', 'win');
  }
  renderBoard(res.board);
}

async function flag(r, c) {
  if (over) return;
  const res = await fetch(`${API}/flag?gameId=${gameId}&row=${r}&col=${c}`, { method: 'POST' }).then(r => r.json());
  if (res.valid) {
    const el = cell(r, c);
    el.className = res.flagged ? 'cell flag' : 'cell';
    document.getElementById('mines').textContent = String(M - res.flagCount).padStart(3, '0');
  }
}

async function onRightClick(r, c) {
  const el = cell(r, c);
  if (el.classList.contains('open') && el.dataset.n) {
    chord(r, c);
  } else {
    flag(r, c);
  }
}

function renderBoard(board) {
  let fc = 0;
  for (let r = 0; r < R; r++) {
    for (let c = 0; c < C; c++) {
      const el = cell(r, c);
      const v = board[r][c];
      el.className = 'cell';
      el.textContent = '';
      el.removeAttribute('data-n');
      if (v >= 0) {
        el.classList.add('open');
        if (v > 0) { el.textContent = v; el.dataset.n = v; }
      } else if (v === -2) {
        el.classList.add('flag');
        fc++;
      } else if (v === -3) {
        el.classList.add('mine');
      }
    }
  }
  document.getElementById('mines').textContent = String(M - fc).padStart(3, '0');
}

async function reset() {
  clearInterval(timer); timer = null; sec = 0; over = false;
  document.getElementById('time').textContent = '000';
  document.getElementById('face').textContent = '😊';
  document.getElementById('msg').className = 'msg';
  const difficulty = document.getElementById('difficulty').value;
  const data = await fetch(`${API}/new?difficulty=${difficulty}`).then(r => r.json());
  gameId = data.gameId;
  R = data.rows; C = data.cols; M = data.mines;
  const g = document.getElementById('grid');
  g.style.gridTemplateColumns = `repeat(${C}, 50px)`;
  g.innerHTML = '';
  for (let i = 0; i < R * C; i++) {
    const cell = document.createElement('div');
    cell.className = 'cell';
    const r = Math.floor(i / C), c = i % C;
    cell.onclick = () => click(r, c);
    cell.oncontextmenu = e => { e.preventDefault(); onRightClick(r, c); };
    g.appendChild(cell);
  }
  renderBoard(data.board);
}

function showMsg(txt, cls) {
  const m = document.getElementById('msg');
  m.textContent = txt;
  m.className = `msg show ${cls}`;
}

document.getElementById('difficulty').onchange = init;
init();
