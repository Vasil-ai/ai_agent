import axios from 'axios';

const client = axios.create({
  baseURL: 'http://localhost:8081/api/agent',
  headers: { 'Content-Type': 'application/json; charset=utf-8' },
  timeout: 120_000, // 2 минуты — LLM может думать долго
});

export default client;
