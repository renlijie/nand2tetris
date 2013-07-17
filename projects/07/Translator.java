import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

class Translator {
  private BufferedReader br;
  private String[] files;
  private int fileIdx = 0;
  private String currFileName;
  private String funcName;

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

  private String GOTO(String label) {
    String s = new StringBuilder()
      .append("@").append(funcName).append("$").append(label).append("\n")
      .append("0;JMP\n")
      .toString();
    return s;
  }

  private String IFGOTO(String label) {
    String s = new StringBuilder()
      .append("@SP\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("@").append(funcName).append("$").append(label).append("\n")
      .append("D;JNE\n")
      .toString();
    return s;
  }

  private String FUNCTION(String f, String k) {
    StringBuilder s = new StringBuilder()
      .append("(").append(f).append(")\n")
      .append("@SP\n")
      .append("A=M\n");

    int kk = Integer.parseInt(k);
    for (int i = 0; i < kk; i += 1) {
      s.append("M=0\n")
        .append("A=A+1\n");
    }

    return s.append("D=A\n")
      .append("@SP\n")
      .append("M=D\n")
      .toString();
  }

  private static final String PRECALL = new StringBuilder()
      .append("D=A\n")
      .append("@SP\n")
      .append("A=M\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("M=M+1\n")
      .append("@LCL\n")
      .append("D=M\n")
      .append("@SP\n")
      .append("A=M\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("M=M+1\n")
      .append("@ARG\n")
      .append("D=M\n")
      .append("@SP\n")
      .append("A=M\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("M=M+1\n")
      .append("@THIS\n")
      .append("D=M\n")
      .append("@SP\n")
      .append("A=M\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("M=M+1\n")
      .append("@THAT\n")
      .append("D=M\n")
      .append("@SP\n")
      .append("A=M\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("M=M+1\n")
      .append("@R13\n")
      .append("D=M\n")
      .toString();

  private String CALL(String f, String n) {
    String c = nextCount();
    return new StringBuilder()
      .append("@SP\n")
      .append("D=M\n")
      .append("@R13\n")
      .append("M=D\n")
      .append("@RET.").append(c).append("\n")
      .append(PRECALL)
      .append("@").append(n).append("\n")
      .append("D=D-A\n")
      .append("@ARG\n")
      .append("M=D\n")
      .append("@SP\n")
      .append("D=M\n")
      .append("@LCL\n")
      .append("M=D\n")
      .append("@").append(f).append("\n")
      .append("0;JMP\n")
      .append("(RET.").append(c).append(")\n")
      .toString();
  }

  private static final String RETURN = new StringBuilder()
      .append("@SP\n")
      .append("A=M-1\n")
      .append("D=M\n")
      .append("@ARG\n")
      .append("A=M\n")
      .append("M=D \n")
      .append("D=A+1\n")
      .append("@SP\n")
      .append("M=D\n")
      .append("@LCL\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("@THAT\n")
      .append("M=D\n")
      .append("@LCL\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("@THIS\n")
      .append("M=D\n")
      .append("@LCL\n")
      .append("AM=M-1\n")
      .append("D=M\n")
      .append("@ARG\n")
      .append("M=D\n")
      .append("@LCL\n")
      .append("AM=M-1\n")
      .append("A=A-1\n")
      .append("D=M\n")
      .append("@R13\n")
      .append("M=D\n")
      .append("@LCL\n")
      .append("A=M\n")
      .append("D=M\n")
      .append("@LCL\n")
      .append("M=D\n")
      .append("@R13\n")
      .append("A=M\n")
      .append("0;JMP\n")
      .toString();

  public Translator(String[] files) {
    this.files = files;
    return;
  }

  private String parseNextCommand() throws Exception {
    String s = nextCommand();
    System.out.println("//" + s);
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
          case "label": return "(" + funcName + "$" + parts[1] + ")\n";
          case "goto": return GOTO(parts[1]);
          case "if-goto": return IFGOTO(parts[1]);
          case "function": { funcName = parts[1]; return FUNCTION(parts[1], parts[2]); }
          case "call": return CALL(parts[1], parts[2]);
          case "return": { return RETURN; }
          default: throw new Exception("bad command! " + parts[0]);
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
      case "static": {
        return new StringBuilder()
          .append("@").append(currFileName).append(".").append(idx).append("\n")
          .append(suffix)
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
      case "static": {
        return new StringBuilder()
          .append("@").append(currFileName).append(".").append(idx).append("\n")
          .append(suffix)
          .toString();
      }
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
    if (br == null)
      if (!open())
        return null;
    String line;
    while(true) {
      line = br.readLine();
      if (line == null) {
        close();
        br = null;
        return nextCommand();
      }
      line = line.replaceAll("//.*", "").trim();
      if (line.length() == 0)
        continue;
      return line;
    }
  }

  private boolean open() {
    try {
      if (br == null && fileIdx != files.length) {
        br = new BufferedReader(new FileReader(files[fileIdx]));
        currFileName = files[fileIdx].replaceAll(".*/", "");
        fileIdx += 1;
        return true;
      } else
        return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
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

  public static void main(String files[]) {
    Translator p = new Translator(files);
    String init = new StringBuilder()
      .append("@256\n")
      .append("D=A\n")
      .append("@SP\n")
      .append("M=D\n")
      .append(p.CALL("Sys.init", "0"))
      .append("0;JMP\n")
      .toString();
    System.out.println(init);
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
