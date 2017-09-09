# celtuce-manifold

Module that provides an implementation for asynchronous commands based on [Manifold][].

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/celtuce-manifold.svg)](https://clojars.org/celtuce-manifold)

Commands are wrapped in Manifold's `deferred` which allows for flexible composition of asynchronous results.

```clj
(require '[celtuce.manifold :refer [commands-manifold]])

(def connector (conn/redis-server "redis://localhost:6379"))
(def cmds (commands-manifold connector))

@(redis/set cmds :foo "bar")
@(redis/get cmds :foo)

(conn/shutdown connector)
```

## License

* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[manifold]: https://github.com/ztellman/manifold