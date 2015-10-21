# Cataclysm

Quick and dirty tester that fully records failures.  Not really designed for performance tesing, Cataclysm leans 
towards learning what is failing and how often.

Run any of the scenarios via:

```
$ ./target/catacylysm run --scenario=RefreshGrant
Running RefreshGrant with 10 threads
Logs /tmp/issues
200 (1490 15tps 1431ms), 4bfa384aa85377b6 NullPointerException (4 0tps 792ms)
```

As it runs, any failing messages or client stacktraces will be logged:

```
$ ls -l /tmp/issues
total 16
-rw-r--r--  1 dblevins  wheel  470 Oct 21 00:23 4bfa384aa85377b6 NullPointerException
-rw-r--r--  1 dblevins  wheel  262 Oct 21 00:29 fc44ecf35b78c2e1 400
```
