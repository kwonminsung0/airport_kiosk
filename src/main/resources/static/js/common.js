// 여러 화면이 함께 쓰는 함수

// 서버가 보내는 시각("2026-11-27T05:44:00")을 "11월 27일 05:44"로 바꾼다.
// 서버의 체크인 시간 안내 문구(M월 d일 HH:mm)와 같은 형식이다.
// new Date()로 바꾸지 않고 글자를 잘라 쓴다. 서버 시각은 출발 공항 현지 시각이라 바꿀 필요가 없고,
// Date로 바꾸면 서머타임이나 UTC 함수에서 시각이 달라질 수 있다
function formatDateTime(isoText) {
    const [date, time] = isoText.split('T');
    const [, month, day] = date.split('-');
    return Number(month) + '월 ' + Number(day) + '일 ' + time.slice(0, 5);
}

// 셀프 체크인 화면 사이에 넘기는 값 (sessionStorage의 키 하나에 JSON으로 저장)
// 키오스크는 여러 승객이 함께 쓰므로 처음으로를 누를 때, 조회 화면에 들어올 때, 체크인이 끝날 때 지운다
const CHECKIN_KEY = 'checkin';

function saveCheckin(data) {
    sessionStorage.setItem(CHECKIN_KEY, JSON.stringify(data));
}

// 저장값이 없으면 null
function loadCheckin() {
    const text = sessionStorage.getItem(CHECKIN_KEY);
    return text ? JSON.parse(text) : null;
}

function clearCheckin() {
    sessionStorage.removeItem(CHECKIN_KEY);
}

// 하단 띠의 '처음으로'(data-home)를 누르면 저장값을 지운 뒤 시작 화면으로 간다
document.querySelectorAll('[data-home]').forEach((link) => {
    link.addEventListener('click', clearCheckin);
});
