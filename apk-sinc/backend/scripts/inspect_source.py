"""Utilitario de inspecao da fonte real de monitoramento.

Rode este script em um ambiente que tenha rota de rede ate
`cli-1237.ddns.a7cloud.net.br:8080` (o sandbox de desenvolvimento usado para
criar este projeto NAO tem essa rota - ver docs/architecture.md).

Uso:
    python backend/scripts/inspect_source.py [URL]

Ele imprime: status HTTP, headers, content-type, os primeiros 2000
caracteres do corpo, e tenta parsear como JSON. Use a saida para ajustar
`app/monitor/real_source.py`.
"""

import sys

import httpx

DEFAULT_URL = "http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/"


def main() -> None:
    url = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_URL
    print(f"Consultando: {url}\n")
    try:
        response = httpx.get(url, timeout=15)
    except Exception as exc:
        print(f"ERRO ao conectar: {exc}")
        sys.exit(1)

    print(f"HTTP status: {response.status_code}")
    print("Headers:")
    for key, value in response.headers.items():
        print(f"  {key}: {value}")

    print(f"\nContent-Type: {response.headers.get('content-type')}")
    body = response.text
    print(f"\nCorpo (primeiros 2000 chars):\n{body[:2000]}")

    try:
        data = response.json()
        print("\nParse JSON bem-sucedido. Estrutura:")
        print(type(data), data if isinstance(data, dict) else data[:3] if isinstance(data, list) else data)
    except ValueError:
        print("\nNao e JSON valido - provavelmente HTML. Ajuste RealSourceClient._parse_html.")


if __name__ == "__main__":
    main()
