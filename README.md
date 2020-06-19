# Chess Engine
For engine specifics, see https://aekusbhathal.com/proj3. 
## Running the code
After cloning, you can either run it in an IDE or directly on the terminal by typing the following after cd'ing into the Root folder:
```
$ javac -cp lib/Chesspresso-lib.jar src/*.java
$ cd src
$ java -cp ".;../lib/Chesspresso-lib.jar" Main
```

## Commands
### /moves
Type this to see all legal moves
### /switch board
Type this to switch from a unicode based board to a text-based board and vice versa. Some terminals don't support the chess unicode characters.
### /faster
Type this to reduce the move time for the computer. This might reduce depth and compromise skill level.
### /slower
Type this to increase the move time for the computer. This might increase depth and skill level.
### /quit
Type this to quit the program
