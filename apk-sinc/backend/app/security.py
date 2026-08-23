"""Autenticacao por chave de API.

O backend expoe dados sensiveis da infraestrutura (quais unidades estao
offline e desde quando) e os tokens FCM dos celulares cadastrados. Enquanto
rodava so na rede local isso era aceitavel; publicado na internet, nao e.

A chave e lida de `API_KEY` no .env e comparada com o header `X-API-Key`.
Se `API_KEY` estiver vazia, a autenticacao fica DESLIGADA - isso mantem o
desenvolvimento local simples (basta nao definir a variavel), mas significa
que em producao a variavel PRECISA estar definida. O endpoint /api/health
fica de fora de proposito, para permitir monitoramento externo do servico.
"""

import secrets

from fastapi import Header, HTTPException, status

from app.config import get_settings


def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    expected = get_settings().api_key
    if not expected:
        return

    # compare_digest evita vazar informacao pelo tempo de comparacao.
    if x_api_key is None or not secrets.compare_digest(x_api_key, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Chave de API ausente ou invalida",
            headers={"WWW-Authenticate": "ApiKey"},
        )
