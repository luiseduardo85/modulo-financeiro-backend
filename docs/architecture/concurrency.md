# Concurrency

Estratégia inicial: optimistic locking, com `@Version` na persistência quando aplicável.

Conflitos concorrentes financeiros não podem sobrescrever dados silenciosamente.
Conflitos normalmente resultam em HTTP 409.
