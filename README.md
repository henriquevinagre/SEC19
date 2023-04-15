# SEC Project 22/23
### Group 19
- Afonso Bernardo (96834)
- César Reis (96849)
- Henrique Vinagre (96869)

## **How to run the program**
### The program can be executed in 2 simple steps:
- Configure the system you want by creating commands (closed server membership, and clients requests) in a txt file like the following:
    - `[number of byzantine servers]` (this **must** be the first line in the file to assign the number of failures to be tolerated)
    - `C [client id]` (creates an instance of a client in order to perform any request he wants to the TES system. It's id is given by `client id`)
    - `S [server id] [port]` (creates an instance of a server running in the specific `port` to participate to the TES system. It's id is given by `server id`)
    - `T C [client id]` (transaction for create_account() service, creates an account for client with `client id`)
    - `T T [source id] [destiny id] [tucs]` (transaction for transfer() service, `source id` transfer `tucs` tucs from its account to the `destiny id` one)
    - `T B [client id] [owner id]` (transaction for check_balance() service, client with `client id` checks the balance of the account given by the `owner id`)
- Run the system you just configured with
```bash
mvn compile exec:java -Dexec.args="[your config file]"
```
- All done! The status of the blockchain system should appear in the standard output! You can also check for some example configuration files in the folder `/configs`

## **How to test our system**
- **(DEPRECATED)** Test classes are present in the test directory `src/test` manipulated by maven. You can test all implemented test cases with:
```bash
mvn compile test
```
- Or if you want a specific one run:
```bash
mvn compile test -Dtest="[your classe of test cases]"
```