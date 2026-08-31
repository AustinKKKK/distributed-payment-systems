import http from 'k6/http';

export const options = {
    vus: 200,
    iterations: 200000,
};

export default function () {
    http.get('http://localhost:8080/accounts/1/balance');
}