import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Translator {
  private BufferedReader br;

  private static final String ADD = new StringBuilder()
    .append("@SP\n")
    .append("AM=M-1\n")
    .append("D=M\n")
    .append("A=A-1\n")
    .append("M=D+M\n")
    .toString();

  private static final String SUB = new StringBuilder()
    .append("@SP\n")
    .append("AM=M-1\n")
    .append("D=M\n")
    .append("A=A-1\n")
    .append("M=M-D\n")
    .toString();

  private static final String NEG = new StringBuilder()
    .append("@SP\n")
    .append("A=M-1\n")
    .append("M=-M\n")
    .toString();

  private static final String AND = new StringBuilder()
    .append("@SP\n")
    .append("AM=M-1\n")
    .append("D=M\n")
    .append("A=A-1\n")
    .append("M=D&M\n")
    .toString();

  private static final String OR = new StringBuilder()
    .append("@SP\n")
    .append("AM=M-1\n")
    .append("D=M\n")
    .append("A=A-1\n")
    .append("M=D|M\n")
    .toString();

  private static final String NOT = new StringBuilder()
    .append("@SP\n")
    .append("A=M-1\n")
    .append("M=!M\n")
    .toString();

  private static final String PUSH = new StringBuilder()
    .append("D=A\n")
    .append("@SP\n")
    .append("A=M\n")
    .append("M=D\n")
    .append("@SP\n")
    .append("M=M+1\n")
    .toString();

  private static final String POP = new StringBuilder()
    .append("D=A\n")
    .append("@R13\n")
    .append("M=D\n")
    .append("@SP\n")
    .append("AM=M-1\n")
    .append("D=M\n")
    .append("@R13\n")
    .append("A=M\n")
    .append("M=D\n")
    .toString();

  private static int count = 0;

  private String nextCount() {
    count += 1;
    return Integer.toString(count);
  }

  private String EQ() {
    String n = nextCount();
    String s = new StringBuilder()
      .append("@SP\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("A=A-1\n")
      .append("D=M-D\n")
      .append("@EQTrue").append(n)
      .append("\nD;JEQ\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=0\n")
      .append("@EQAfter").append(n)
      .append("\n0;JMP\n")
      .append("(EQTrue").append(n).append(")\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=-1\n")
      .append("(EQAfter").append(n).append(")\n")
      .toString();
    return s;
  }

  private String GT() {
    String n = nextCount();
    String s = new StringBuilder()
      .append("@SP\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("A=A-1\n")
      .append("D=M-D\n")
      .append("@GTTrue").append(n)
      .append("\nD;JGT\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=0\n")
      .append("@GTAfter").append(n)
      .append("\n0;JMP\n")
      .append("(GTTrue").append(n).append(")\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=-1\n")
      .append("(GTAfter").append(n).append(")\n")
      .toString();
    return s;
  }

  private String LT() {
    String n = nextCount();
    String s = new StringBuilder()
      .append("@SP\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("A=A-1\n")
      .append("D=M-D\n")
      .append("@LTTrue").append(n)
      .append("\nD;JLT\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=0\n")
      .append("@LTAfter").append(n)
      .append("\n0;JMP\n")
      .append("(LTTrue").append(n).append(")\n")
      .append("@SP\n")
      .append("A=M-1\n")
      .append("M=-1\n")
      .append("(LTAfter").append(n).append(")\n")
      .toString();
    return s;
  }

  public Translator(String file) {
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
    return;
  }

  private String parseNextCommand() throws Exception {
    String s = nextCommand();
    if (s == null)
      return null;
    switch (s) {
      case "add": {
        return ADD;
      }
      case "sub": {
        return SUB;
      }
      case "neg": {
        return NEG;
      }
      case "eq": {
        return EQ();
      }
      case "gt": {
        return GT();
      }
      case "lt": {
        return LT();
      }
      case "and": {
        return AND;
      }
      case "or": {
        return OR;
      }
      case "not": {
        return NOT;
      }
      default: {
        String[] parts = s.split(" ");
        if (parts.length == 0)
          throw new Exception("bad command!");
        switch (parts[0]) {
          case "push": return parsePush(parts[1], parts[2]);
          case "pop": return parsePop(parts[1], parts[2]);
          default: throw new Exception("bad command!");
        }
      }
    }
  }

  private String parsePop(String base, String idx) throws Exception{
    String suffix = new StringBuilder()
          .append("D=M\n")
          .append("@").append(idx).append("\n")
          .append("A=D+A\n")
          .append(POP)
          .toString();
    switch (base) {
      case "local": {
        return new StringBuilder()
          .append("@LCL\n")
          .append(suffix)
          .toString();
      }
      case "argument": {
        return new StringBuilder()
          .append("@ARG\n")
          .append(suffix)
          .toString();
      }
      case "this": {
        return new StringBuilder()
          .append("@THIS\n")
          .append(suffix)
          .toString();
      }
      case "that": {
        return new StringBuilder()
          .append("@THAT\n")
          .append(suffix)
          .toString();
      }
      case "pointer": {
        if (idx.equals("0"))
          return new StringBuilder()
            .append("@THIS\n")
            .append(POP)
            .toString();
        else
          return new StringBuilder()
            .append("@THAT\n")
            .append(POP)
            .toString();
      }
      case "temp": {
        return new StringBuilder()
          .append("@R5\n")
          .append("D=A\n")
          .append("@").append(idx).append("\n")
          .append("A=D+A\n")
          .append(POP)
          .toString();
      }
      default: throw new Exception("bad command!");
    }
  }

  private String parsePush(String base, String idx) throws Exception{
    String suffix = new StringBuilder()
          .append("D=M\n")
          .append("@").append(idx).append("\n")
          .append("A=D+A\n")
          .append("A=M\n")
          .append(PUSH)
          .toString();
    switch (base) {
      case "constant": {
        return new StringBuilder()
          .append("@").append(idx).append("\n")
          .append(PUSH)
          .toString();
      }
      case "local": {
        return new StringBuilder()
          .append("@LCL\n")
          .append(suffix)
          .toString();
      }
      case "argument": {
        return new StringBuilder()
          .append("@ARG\n")
          .append(suffix)
          .toString();
      }
      case "this": {
        return new StringBuilder()
          .append("@THIS\n")
          .append(suffix)
          .toString();
      }
      case "that": {
        return new StringBuilder()
          .append("@THAT\n")
          .append(suffix)
          .toString();
      }
      case "pointer": {
        if (idx.equals("0"))
          return new StringBuilder()
            .append("@THIS\n")
            .append("A=M\n")
            .append(PUSH)
            .toString();
        else
          return new StringBuilder()
            .append("@THAT\n")
            .append("A=M\n")
            .append(PUSH)
            .toString();
      }
      case "temp": {
        return new StringBuilder()
          .append("@R5\n")
          .append("D=A\n")
          .append("@").append(idx).append("\n")
          .append("A=D+A\n")
          .append("A=M\n")
          .append(PUSH)
          .toString();
      }
      default: throw new Exception("bad command!");
    }
  }

  private String nextCommand() throws IOException {
    String line;
    while(true) {
      line = br.readLine();
      if (line == null) {
        close();
        return null;
      }
      line = line.replaceAll("//.*", "").trim();
      if (line.length() == 0)
        continue;
      return line;
    }
  }

  private void close() {
    try {
      if (br != null)
        br.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String args[]) {
    Translator p = new Translator(args[0]);
    String s;
    try {
      while(true) {
        s = p.parseNextCommand();
        if (s == null)
          return;
        System.out.println(s);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
