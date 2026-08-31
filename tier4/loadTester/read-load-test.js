import http from 'k6/http';

export const options = {
  vus: 200,
  duration: '30s',
};

export default function () {
  const randomId = Math.floor(Math.random() * 1000) + 1;
  http.get(`http://localhost:8080/payments/${randomId}`);
}