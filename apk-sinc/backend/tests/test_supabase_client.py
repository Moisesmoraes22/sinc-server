"""Teste de regressao: versoes antigas do supabase-py (<= 2.9.x) validam a
API key com uma regex que exige formato JWT (com pontos), e rejeitam com
"Invalid API key" o novo formato de chave do Supabase (`sb_secret_...` /
`sb_publishable_...`), que nao tem pontos. Isso foi descoberto ao conectar
o backend a um projeto Supabase real criado em 2026 (ja usando o novo
formato) - requirements.txt foi atualizado para supabase>=2.10 por causa
disso. Este teste garante que a versao instalada aceita o novo formato,
sem depender de rede real (so a validacao de formato acontece na
construcao do cliente, antes de qualquer chamada HTTP)."""

from supabase import create_client


def test_client_accepts_new_style_secret_key_format():
    # Nao e uma chave real - so precisa ter o formato sb_secret_/sb_publishable_
    # (sem pontos) que as versoes antigas do supabase-py rejeitavam.
    client = create_client("https://example.supabase.co", "sb_secret_fake_key_for_format_test_only")
    assert client is not None
