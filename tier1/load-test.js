import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 500,
//  iterations: 500000,
  duration: '30s',
};

export default function () {
  // unique idempotency key for each request
  const key = `key-${__VU}-${__ITER}`;

  const payload = JSON.stringify({
    idempotencyKey: key,
    fromAccount: 1,
    toAccount: 2,
    amount: 1000000,
  });

  const params = { headers: { 'Content-Type': 'application/json' } };

  const res = http.post('http://localhost:8080/payments', payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
