# SEC Project 22/23
### Group 19
- Afonso Bernardo (96834)
- César Reis (96849)
- Henrique Vinagre (96869)

## **How to run the program**
### The program can be executed in 2 simple steps:
- Configure the system you want (closed server membership, and clients) in a txt file like the following:
    - `[number of byzantine servers]` (this **must** be the first line in the file)
    - `C [message]` (instance of a client sending an *append* request to the blockchain system)
    - `S [port]` (instance of a server of the system running in the port defined (localhost))
    - `# [comment]` (add a relevant comment to the config file)
- Run the system you just configured with
```bash
mvn compile exec:java -Dexec.args="[your config file]"
```
- All done! The status of the blockchain system should appear in the standard output! You can also check for some example configuration files in the folder `/configs`

## **How to test our system**
- Test classes are present in the test directory `src/test` manipulated my maven. You can test all implemented test cases with:
```bash
mvn compile test
```
- Or if you want a specific one run:
```bash
mvn compile test -Dtest="[you classe of test cases]"
```