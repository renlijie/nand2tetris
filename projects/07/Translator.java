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

  private static final String PUSH_CONST = new StringBuilder()
    .append("D=A\n")
    .append("@SP\n")
    .append("A=M\n")
    .append("M=D\n")
    .append("@SP\n")
    .append("M=M+1\n")
    .toString();

  public Translator(String file) {
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
    return;
  }

  public String parseNextCommand() throws Exception {
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
        return parsePush(s);
      }
    }
  }

  public String parsePush(String s) throws Exception{
    String[] parts = s.split(" ");
    if (parts.length == 0)
      throw new Exception("bad command!");
    switch (parts[0]) {
      case "push": {
        switch (parts[1]) {
          case "constant": {
            return new StringBuilder()
              .append("@").append(parts[2]).append("\n")
              .append(PUSH_CONST)
              .toString();
          }
          default: throw new Exception("bad command!");
        }
      }
      default: throw new Exception("bad command!");
    }
  }

  public String nextCommand() throws IOException {
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
