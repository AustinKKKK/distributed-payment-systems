import http from 'k6/http';

export const options = {
  vus: 50,
  iterations: 50,   // 딱 50번, vus 50이니 한 방에 다 동시에
};

export default function () {
  const payload = JSON.stringify({
    idempotencyKey: 'same-key-race-test',   // ← 전부 똑같은 키
    fromAccount: 1,
    toAccount: 2,
    amount: 1000000,
  });

  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post('http://localhost:8080/payments', payload, params);

  console.log(`status: ${res.status}`);
}