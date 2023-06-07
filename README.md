# Secrets
## Simple password manager with browser UI <br/>
## Written in Clojure/ClojureScript, server is on Babashka, frontend is React/Reagent <br/>
### Server depends on:
- #### [HTTP Kit](https://github.com/http-kit/http-kit)
- #### [SQLite](https://www.sqlite.org/index.html)
- #### [HoneySQL](https://github.com/seancorfield/honeysql)
- #### [Next.JDBC](https://github.com/seancorfield/next-jdbc)
- #### [Askonomm.Ruuter](https://github.com/askonomm/ruuter)
- #### [Cognitect.Test-Runner](https://github.com/cognitect-labs/test-runner)

### Front-end depends on:
- #### [Shadow-CLJS](https://github.com/thheller/shadow-cljs)
- #### [Reagent](https://github.com/reagent-project/reagent)
- #### [Core.Async](https://github.com/clojure/core.async)
- #### [CLJS-Http](https://github.com/r0man/cljs-http)
- #### [Askonomm.Ruuter](https://github.com/askonomm/ruuter)
- #### [Bulma.CSS](https://bulma.io)

## To run the server on your PC:
1. Install [Clojure](https://clojure.org/guides/getting_started)
2. Install [Babashka](https://book.babashka.org)
3. Clone the [repo](https://)
4. Open dir with cloned repo in your terminal
```console
cd "/path/to/cloned/repo"
```
4. Run the command to start server and you can specify a port by using '-p' option
and static server port by '-sport':
```console
bb -m app -p 2525 -sport 5001
```
5. Open new window in your browser and enter the url  http://localhost:5001 to run UI.

## If you want to compile the frontend from scratch, you can use the command in terminal from repo root dir:
```console
bb create-static    
``` 

