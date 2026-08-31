import http from 'k6/http';

export const options = {
  vus: 200,
  duration: '30s',
};

export default function () {
  const key = `key-${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    idempotencyKey: key,
    fromAccount: 1,
    toAccount: 2,
    amount: 1000,
  });
  const params = { headers: { 'Content-Type': 'application/json' } };
  http.post('http://localhost:8080/payments', payload, params);
}
