const DEFAULT_ERROR = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';

const form = document.getElementById('lookup-form');
const button = document.getElementById('lookup-button');
const message = document.getElementById('message');
const result = document.getElementById('result');

form.addEventListener('submit', async (event) => {
    event.preventDefault(); // 페이지가 새로고침되지 않게 한다
    hideMessage();
    result.hidden = true;
    button.disabled = true; // 응답을 기다리는 동안 두 번 누르지 못하게 한다

    try {
        const response = await fetch('/api/checkin/reservations/lookup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                pnr: document.getElementById('pnr').value,
                lastName: document.getElementById('last-name').value
            })
        });
        const body = await response.json();
        if (response.ok) {
            showReservation(body);
        } else {
            showMessage(body.message || DEFAULT_ERROR);
        }
    } catch (error) {
        // 서버에 연결할 수 없거나 응답이 JSON이 아닌 경우
        showMessage(DEFAULT_ERROR);
    } finally {
        button.disabled = false;
    }
});

function showReservation(reservation) {
    setText('result-pnr', reservation.pnr);
    setText('result-name', reservation.lastName + ' ' + reservation.firstName);
    setText('result-flight-no', reservation.flightNo);
    setText('result-route', reservation.depAirport + ' → ' + reservation.arrAirport);
    setText('result-dep-time', formatDateTime(reservation.depTime)); // common.js
    setText('result-gate', reservation.gate);
    result.hidden = false;
}

// innerHTML 대신 textContent를 사용해 값이 HTML로 실행되지 않게 한다
function setText(id, text) {
    document.getElementById(id).textContent = text;
}

function showMessage(text) {
    message.textContent = text;
    message.hidden = false;
}

function hideMessage() {
    message.textContent = '';
    message.hidden = true;
}
