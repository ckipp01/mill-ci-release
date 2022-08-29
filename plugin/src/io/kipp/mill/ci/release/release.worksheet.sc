import os.CommandResult

val version = os.proc("gpg", "--version").call()

val echo = os.proc("echo", "Zm9vYmFyCg==").spawn()

val decoded = os.proc("base64", "--decode").call(stdin = echo.stdout)

Option(System.getenv("foobar")).nonEmpty

val x = Seq("hi", "there", "you")

x.mkString(" - ", "\n - ", "")
