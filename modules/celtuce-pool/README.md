# celtuce-pool

Module that provides [connection pooling][conn-pool] for `connector`s.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/celtuce-pool.svg)](https://clojars.org/celtuce-pool)

Pooling connections is particularly useful for transactions or long running time commands
(so that other threads can use different connections in the meantime).

### Connection Pool for Synchronous Commands

```clj
(require '[celtuce.connector :as conn]
         '[celtuce.pool :as pool]
         '[celtuce.commands :as redis])

(def connector (conn/redis-server "redis://localhost:6379"))

;; Create a pool of connections from a connector
;; and specify a command function to use on it
(def sync-conn-pool (pool/conn-pool connector conn/commands-sync))

;; Use a connection from the pool, call command function on it
;; and binds the resulting commands to cmds
(pool/with-conn-pool sync-conn-pool cmds
  (redis/multi cmds)
  (redis/set   cmds :a 1)
  (redis/set   cmds :b 2)
  (redis/get   cmds :a)
  (redis/get   cmds :b)
  (redis/exec  cmds))

;; When you are done using the connection pool
(pool/close sync-conn-pool)
(conn/shutdown connector)
```

### Connection Pool for Asynchronous Commands

```clj
(require '[celtuce.connector :as conn]
         '[celtuce.pool :as pool]
         '[celtuce.manifold :as celtuce-manifold]
         '[manifold.deferred :as d]
         '[celtuce.commands :as redis])

(def connector (conn/redis-server "redis://localhost:6379"))

;; Create a pool of connections from a connector
;; and specify a command function to use on it
(def async-conn-pool (pool/conn-pool connector celtuce-manifold/commands-manifold))

;; Use a connection from the pool, call command function on it
;; and binds the resulting commands to cmds
(let [result (pool/with-conn-pool* async-conn-pool cmds c
               (d/chain (redis/multi cmds)
                 (redis/set cmds :a 1)
                 (redis/set cmds :b 2)
                 (redis/get cmds :a)
                 (redis/get cmds :b)
                 (fn [_] (redis/exec cmds))
                 (fn [res] (pool/return-conn async-conn-pool c) res)))]
  (= ["OK" "OK" 1 2] @result))

  ;; When you are done using the connection pool
(pool/close async-conn-pool)
(conn/shutdown connector)
```


### Connection Pool Configuration
Note that `conn-pool` can take an optional pool configuration:

```clj
(pool/conn-pool connector conn/commands-sync
  {:max-total 8, :max-idle 8, :min-idle 0})
```


```clj
(pool/conn-pool connector celtuce-manifold/commands-manifold
  {:max-total 8, :max-idle 8, :min-idle 0})
```

## License

* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[conn-pool]: https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling
