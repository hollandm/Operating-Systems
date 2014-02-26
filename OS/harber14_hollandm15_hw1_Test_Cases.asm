#This file contains all of the the pidgin op codes for testing purposes.
#If it executes correctly, the expected results will be:
#r0=58 r1=27 r2=8 r3=8 r4=6

SET R0 8
SET R1 5

ADD R3 R0 R1
SUB R4 R0 R1
MUL R4 R0 R1
DIV R3 R4 R1

PUSH R0
PUSH R1
PUSH R4

POP R2
POP R0

SAVE R3 R4
LOAD R2 R4

BRANCH here

SET R0 5 #This should NOT be executed

:here

SET R0 27
COPY R1 R0

BLT R2 R3 lessThan
SET R4 4 #This should be executed

BNE R2 R0 notEqual

SET R0 100 #This should NOT be executed

:notEqual
SET R0 58

:lessThan
SET R4 6
