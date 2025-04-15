⚠️ I have used JDK 7

⭐ first start the server side

```js
javac *.java 
```

```js
rmic SudokuImpl    
rmic FabSudokuImpl
```

```js
java SudokuServer.java <server-ip>
```

⭐ then  start the client side
```js
javac *.java 
```
```js
java SudokuClient.java <server-ip>
```
