@echo off
REM Sobe o backend do APK SINC. Clique duas vezes neste arquivo para iniciar.
REM Funciona de qualquer lugar que a pasta "backend" esteja (nao depende de caminho fixo).

cd /d "%~dp0"

if not exist ".venv\Scripts\activate.bat" (
    echo [ERRO] Ambiente virtual .venv nao encontrado nesta pasta.
    echo Rode primeiro: python -m venv .venv ^&^& .venv\Scripts\Activate.ps1 ^&^& pip install -r requirements.txt
    pause
    exit /b 1
)

call .venv\Scripts\activate.bat

echo Subindo o backend APK SINC em http://0.0.0.0:8001 ...
echo Deixe esta janela aberta. Feche-a para parar o servidor.
echo.

REM Sem --reload de proposito: essa flag e para desenvolvimento (reinicia o
REM processo sozinho a cada mudanca de arquivo detectada na pasta toda,
REM incluindo .venv). Rodando 24/7 como servico, isso e um risco real - um
REM reinicio automatico que trava faz o ciclo de checagem parar de vez, sem
REM nenhum erro visivel (foi exatamente esse o padrao observado: unidades
REM congeladas ha horas, sem mensagem de erro no log).
uvicorn app.main:app --host 0.0.0.0 --port 8001

pause
