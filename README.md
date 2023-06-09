# Key value store

A key-value store full stack application based on the following libraries:

- [fs2](https://fs2.io/)
- [http4s](https://http4s.org/)
- [ff4s](https://github.com/buntec/ff4s)

Start the application using the following commands:

1. `sbt backend/run` for the backend.
2. `sbt frontend/fastLinkJS` and serve the `frontend/index.html` with the tool of your choice.
   For example, [Live server](https://www.npmjs.com/package/live-server) can be used by executing
   the shell script `frontend/start-dev-server.sh`.
