// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, the
// program clears the screen, i.e. writes "white" in every pixel.

// Put your code here.

(LOOP)
  // save screen pixel's address to R0
  @SCREEN
  D=A
  @R0
  M=D

  // get key code
  @KBD
  D=M
  @WHITE
  D;JEQ

(BLACK)
  // inc R0 by 1
  @R0
  A=M
  M=-1
  D=A+1
  @R0
  M=D

  // check if drawing is finished.
  @KBD
  D=D-A
  @BLACK
  D;JLT

  @LOOP
  0;JMP

(WHITE)
  @R0
  A=M
  M=0
  D=A+1
  @R0
  M=D

  @KBD
  D=D-A
  @WHITE
  D;JLT

  @LOOP
  0;JMP
