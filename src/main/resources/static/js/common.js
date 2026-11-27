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
