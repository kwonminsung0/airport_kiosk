const DEFAULT_ERROR = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
const SVG_NS = 'http://www.w3.org/2000/svg';

const message = document.getElementById('message');
const notice = document.getElementById('notice');
const fuselage = document.getElementById('fuselage');
const seatMap = document.getElementById('seat-map');
const nextButton = document.getElementById('next-button');
const nextHint = document.getElementById('next-hint');

// 조회 화면에서 저장한 예약 정보 (common.js). 없으면 조회부터 다시 한다
const checkin = loadCheckin();
let selectedSeat = null; // 지금 고른 좌석 버튼

if (checkin === null) {
    // replace: 뒤로 가기로 이 화면에 다시 오지 않게 방문 기록을 바꾼다
    location.replace('/checkin/lookup.html');
} else {
    showReservation();
    loadSeats();
}

function showReservation() {
    setText('summary-name', checkin.lastName + ' ' + checkin.firstName);
    setText('summary-flight-no', checkin.flightNo);
    setText('summary-route', checkin.depAirport + ' → ' + checkin.arrAirport);
    setText('summary-dep-time', formatDateTime(checkin.depTime)); // common.js
}

async function loadSeats() {
    try {
        const response = await fetch('/api/checkin/flights/' + encodeURIComponent(checkin.flightId) + '/seats');
        const body = await response.json();
        if (response.ok) {
            drawSeatMap(body);
        } else {
            showMessage(body.message || DEFAULT_ERROR);
        }
    } catch (error) {
        // 서버에 연결할 수 없거나 응답이 JSON이 아닌 경우
        showMessage(DEFAULT_ERROR);
    }
}

// 비행기를 옆으로 눕혀 그린다: 기수가 왼쪽, 행 번호는 왼쪽에서 오른쪽으로 1, 2, 3 …
// 앞을 보고 앉은 승객의 왼쪽 창가(A)가 화면 아래, 오른쪽 창가(마지막 열)가 화면 위다.
// 좌석 버튼은 HTML에 1A, 1B … 1F, 2A … 순서로 넣고(Tab 순서·화면 낭독 순서), 보이는 자리만 grid-row/grid-column으로 정한다
function drawSeatMap(seatMapResponse) {
    const rows = seatMapResponse.seatRows;
    const cols = seatMapResponse.seatCols;
    const aisle = Math.floor(cols / 2); // 통로 위치는 데이터에 없고 열 수의 가운데로 계산한다 (6열이면 C와 D 사이)
    const letters = 'ABCDEFGHIJ'.slice(0, cols).split(''); // 열 문자는 data.sql과 같이 A부터 차례로

    // 화면 줄 번호: 위에서부터 마지막 열 … 통로 오른쪽 열, 통로 줄, 통로 왼쪽 열 … A
    const lineOf = (index) => (index >= aisle ? cols - index : cols - index + 1);
    const aisleLine = cols - aisle + 1;

    seatMap.style.gridTemplateColumns = 'var(--seat-label-w) repeat(' + rows + ', var(--seat-w))';
    seatMap.style.gridTemplateRows = 'repeat(' + (cols - aisle) + ', var(--seat-h)) var(--aisle-h) repeat(' + aisle + ', var(--seat-h))';

    // 열 문자(F … A)와 "◀ 앞쪽", 행 번호는 화면에만 보이는 안내라 화면 낭독기에서는 숨긴다
    letters.forEach((letter, index) => addLabel('seat-col-label', letter, lineOf(index), 1));
    const front = addLabel('seat-front', '', aisleLine, 1);
    const frontText = document.createElement('span');
    frontText.textContent = '◀ 앞쪽';
    front.appendChild(frontText);
    for (let row = 1; row <= rows; row++) {
        addLabel('seat-row-label', String(row), aisleLine, row + 1);
    }

    seatMapResponse.seats.forEach((seat) => {
        const row = parseInt(seat.seatNo, 10);
        const letter = seat.seatNo.slice(-1);
        const index = letters.indexOf(letter);
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'seat';
        button.dataset.seatId = seat.seatId;
        button.dataset.seatNo = seat.seatNo;
        button.dataset.kind = seatKind(index, cols, aisle);
        button.style.gridRow = lineOf(index);
        button.style.gridColumn = row + 1;
        // 다른 승객에게 배정된 좌석은 누를 수 없고 Tab으로도 가지 않는다
        button.disabled = seat.status !== 'AVAILABLE';
        if (button.disabled) {
            button.classList.add('seat-taken');
            button.appendChild(icon('icon-x'));
        }
        button.setAttribute('aria-pressed', 'false');
        updateSeatLabel(button);
        seatMap.appendChild(button);
    });

    fuselage.hidden = false;
}

function addLabel(className, text, gridRow, gridColumn) {
    const label = document.createElement('span');
    label.className = className;
    label.textContent = text;
    label.style.gridRow = gridRow;
    label.style.gridColumn = gridColumn;
    label.setAttribute('aria-hidden', 'true');
    seatMap.appendChild(label);
    return label;
}

// 양 끝 열은 창가, 통로 양옆은 통로, 나머지는 가운데
function seatKind(index, cols, aisle) {
    if (index === 0 || index === cols - 1) {
        return '창가';
    }
    if (index === aisle - 1 || index === aisle) {
        return '통로';
    }
    return '가운데';
}

// 화면 낭독기가 "5A 창가 좌석, 선택 가능"처럼 읽는다
function updateSeatLabel(button) {
    let state = '선택 가능';
    if (button.disabled) {
        state = '선택 불가';
    } else if (button.getAttribute('aria-pressed') === 'true') {
        state = '선택됨';
    }
    button.setAttribute('aria-label', button.dataset.seatNo + ' ' + button.dataset.kind + ' 좌석, ' + state);
}

function icon(id) {
    const svg = document.createElementNS(SVG_NS, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('aria-hidden', 'true');
    const use = document.createElementNS(SVG_NS, 'use');
    use.setAttribute('href', '#' + id);
    svg.appendChild(use);
    return svg;
}

// 좌석을 누르면 고르기만 한다. 실제 배정은 4단계 체크인 확정(POST /api/checkin)에서 한다
seatMap.addEventListener('click', (event) => {
    const button = event.target.closest('.seat');
    if (button === null || button.disabled) {
        return;
    }
    if (selectedSeat !== null) {
        selectedSeat.setAttribute('aria-pressed', 'false');
        selectedSeat.replaceChildren();
        updateSeatLabel(selectedSeat);
    }
    selectedSeat = button;
    button.setAttribute('aria-pressed', 'true');
    button.replaceChildren(icon('icon-check'));
    updateSeatLabel(button);

    setText('picked-seat', button.dataset.seatNo);
    setText('picked-kind', button.dataset.kind + ' 좌석');
    nextButton.disabled = false;
    nextHint.hidden = true;
    notice.hidden = true;
});

// 고른 좌석 번호(seatId)를 저장한다. 체크인 확정 화면은 4단계에서 만든다
nextButton.addEventListener('click', () => {
    if (selectedSeat === null) {
        return;
    }
    checkin.seatId = Number(selectedSeat.dataset.seatId);
    saveCheckin(checkin);
    notice.textContent = selectedSeat.dataset.seatNo + ' 좌석을 골랐습니다. 확정은 다음 단계에서 이어집니다.';
    notice.hidden = false;
});

// innerHTML 대신 textContent를 사용해 값이 HTML로 실행되지 않게 한다
function setText(id, text) {
    document.getElementById(id).textContent = text;
}

function showMessage(text) {
    message.textContent = text;
    message.hidden = false;
}
