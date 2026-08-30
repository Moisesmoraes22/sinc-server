' Inicia o backend do OMG SINC sem mostrar nenhuma janela de console.
'
' A opcao "Oculto" do proprio Agendador de Tarefas do Windows apresenta o
' erro "Um ou mais dos argumentos especificados nao sao validos" em
' algumas configuracoes (mesmo trocando "Configurar para" para versoes
' mais novas). Este script contorna isso rodando o start_backend.bat via
' WScript.Shell.Run com o parametro de janela = 0 (oculta), que nao
' depende daquela opcao com problema.
'
' Uso: aponte a Acao da Tarefa Agendada para "wscript.exe" com este
' arquivo como argumento, em vez de apontar direto para start_backend.bat.
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = scriptDir & "\start_backend.bat"

Set shell = CreateObject("WScript.Shell")
shell.Run """" & batPath & """", 0, False
