# URBANPAY

O Urbanpay é um banco digital brasileiro fictício, relevante por oferecer conta corrente e de investimentos gratuita, gateway de cobranças, cartão de crédito sem anuidade, Pix, plataforma de investimentos diversificada (com Tesouro, bolsa, cripto), crédito (imóveis, veículos, consignado) e porquinhos.<br/>
Este repositório tem como objetivo estudar e desenvolver o código-fonte e os recursos necessários para implementar e testar as funcionalidades da plataforma.

## Capturas de Tela

## Objetivo

- Estudar a arquitetura de um banco digital utilizando tecnologias modernas do ecossistema JavaScript
- Criar uma plataforma que tentasse ao máximo solucionar alguns problemas de segurança do mundo real envolvendo roubos, transferencias indevidas e etc
- Implementar fluxos reais de autenticação, segurança e gestão de usuários
- Desenvolver funcionalidades bancárias comuns, como:
    Conta corrente digital
    Pagamentos via Pix
    Cartões e controle financeiro
    Porquinhos (metas e reservas financeiras)
    Plataforma de investimentos (Tesouro, renda variável e cripto – de forma simulada)
- Explorar conceitos de segurança, como criptografia de senhas, tokens e proteção de dados
- Praticar boas práticas de código, organização de projeto, versionamento e escalabilidade
- Servir como base para testes, estudos de UX/UI e evolução técnica, sem fins comerciais

## Arquitetura

```
apps/web (Next.js) ──┐   BFF: telas + rotas /api que guardam os tokens em cookie httpOnly
apps/mobile (Expo) ──┤
                     ▼
        services/core (Java 21 + Spring Boot)   identidade + ledger de partidas dobradas
                     ▼
                PostgreSQL
```

- **Java (`services/core`)**: tudo que envolve dinheiro, identidade e regras de negócio.
  Detalhes em [services/core/README.md](services/core/README.md).
- **TypeScript (`apps/web`)**: interface e BFF (Backend for Frontend). As rotas `/api` do Next.js
  não têm regra de negócio: autenticam via cookie, chamam o core e adaptam a resposta para a tela.
  O `src/proxy.ts` protege as páginas validando o JWT com a chave pública do core.

## Como rodar

```bash
cp .env.example .env
docker compose up -d postgres
(cd services/core && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run)
npm install && npm run dev
```

Web em http://localhost:3000 · API em http://localhost:8080/swagger-ui.html

## Dependências

O projeto foi desenvolvido com as seguintes dependências:

- **[Next.js](https://nextjs.org)** – v16.2.1
- **[Bcrypt](https://www.npmjs.com/package/bcrypt)** – ^3.0.2  
- **[jose](https://www.npmjs.com/package/jose)** – ^6.1.0
- **Java 21**, **Spring Boot 4.1**, **Spring Modulith**
- **PostgreSQL 16**, **Flyway**, **Testcontainers**

by caiothedev<br/>
#bank #nextjs #fintech #java #finance