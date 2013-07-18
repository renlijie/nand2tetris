import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Translate virtual machine code (.vm) to assembly code (.asm).
class Translator {
  private static final String ADD =
    "@SP\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "A=A-1\n" +
    "M=D+M\n";

  private static final String SUB =
    "@SP\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "A=A-1\n" +
    "M=M-D\n";

  private static final String NEG =
    "@SP\n" +
    "A=M-1\n" +
    "M=-M\n";

  private static final String AND =
    "@SP\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "A=A-1\n" +
    "M=D&M\n";

  private static final String OR =
    "@SP\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "A=A-1\n" +
    "M=D|M\n";

  private static final String NOT =
    "@SP\n" +
    "A=M-1\n" +
    "M=!M\n";

  // push D -> *SP++
  private static final String PUSH =
    "@SP\n" +
    "A=M\n" +
    "M=D\n" +
    "@SP\n" +
    "M=M+1\n";

  // pop *SP-- -> *D
  private static final String POP =
    "@R13\n" +
    "M=D\n" +
    "@SP\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "@R13\n" +
    "A=M\n" +
    "M=D\n";

  private static final String RETURN =
    // *(LCL - 5) -> R13
    "@LCL\n" +
    "D=M\n" +
    "@5\n" +
    "A=D-A\n" +
    "D=M\n" +
    "@R13\n" +
    "M=D\n" +
    // *(SP - 1) -> *ARG
    "@SP\n" +
    "A=M-1\n" +
    "D=M\n" +
    "@ARG\n" +
    "A=M\n" +
    "M=D \n" +
    // ARG + 1 -> SP
    "D=A+1\n" +
    "@SP\n" +
    "M=D\n" +
    // *(LCL - 1) -> THAT; LCL--
    "@LCL\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "@THAT\n" +
    "M=D\n" +
    // *(LCL - 1) -> THIS; LCL--
    "@LCL\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "@THIS\n" +
    "M=D\n" +
    // *(LCL - 1) -> ARG; LCL--
    "@LCL\n" +
    "AM=M-1\n" +
    "D=M\n" +
    "@ARG\n" +
    "M=D\n" +
    // *(LCL - 1) -> LCL
    "@LCL\n" +
    "A=M-1\n" +
    "D=M\n" +
    "@LCL\n" +
    "M=D\n" +
    // R13 -> A
    "@R13\n" +
    "A=M\n" +
    "0;JMP\n";

  private static int count = 0;

  private BufferedReader br;
  private String[] files;
  private int fileIdx = 0;
  private String currFileName;
  private String funcName;

  public Translator(String[] files) {
    for (String file : files) {
      System.err.println("file: " + file);
    }
    this.files = files;
    return;
  }

  private String nextCount() {
    count += 1;
    return Integer.toString(count);
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

  private String parsePop(String base, String idx) throws Exception {
    switch (base) {
      case "local": {
        return
          "@LCL\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "D=D+A\n" +
          POP;
      }
      case "argument": {
        return
          "@ARG\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "D=D+A\n" +
          POP;
      }
      case "this": {
        return
          "@THIS\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "D=D+A\n" +
          POP;
      }
      case "that": {
        return
          "@THAT\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "D=D+A\n" +
          POP;
      }
      case "pointer": {
        if (idx.equals("0"))
          return
            "@THIS\n" +
            "D=A\n" +
            POP;
        else
          return
            "@THAT\n" +
            "D=A\n" +
            POP;
      }
      case "static": {
        return
          "@" + currFileName + "." + idx + "\n" +
          "D=A\n" +
          POP;
      }
      case "temp": {
        return
          "@R5\n" +
          "D=A\n" +
          "@" + idx + "\n" +
          "D=D+A\n" +
          POP;
      }
      default: throw new Exception("bad command!");
    }
  }

  private String parsePush(String base, String idx) throws Exception {
    switch (base) {
      case "local": {
        return
          "@LCL\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "A=D+A\n" +
          "D=M\n" +
          PUSH;
      }
      case "argument": {
        return
          "@ARG\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "A=D+A\n" +
          "D=M\n" +
          PUSH;
      }
      case "this": {
        return
          "@THIS\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "A=D+A\n" +
          "D=M\n" +
          PUSH;
      }
      case "that": {
        return
          "@THAT\n" +
          "D=M\n" +
          "@" + idx + "\n" +
          "A=D+A\n" +
          "D=M\n" +
          PUSH;
      }
      case "pointer": {
        if (idx.equals("0"))
          return
            "@THIS\n" +
            "D=M\n" +
            PUSH;
        else
          return
            "@THAT\n" +
            "D=M\n" +
            PUSH;
      }
      case "constant": {
        return
          "@" + idx + "\n" +
          "D=A\n" +
          PUSH;
      }
      case "static": {
        return
          "@" + currFileName + "." + idx + "\n" +
          "D=M\n" +
          PUSH;
      }
      case "temp": {
        return
          "@R5\n" +
          "D=A\n" +
          "@" + idx + "\n" +
          "A=D+A\n" +
          "D=M\n" +
          PUSH;
      }
      default: throw new Exception("bad command!");
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

  private String EQ() {
    String n = nextCount();
    String s =
      "@SP\n" +
      "AM=M-1\n" +
      "D=M\n" +
      "A=A-1\n" +
      "D=M-D\n" +
      "@EQ.true." + n + "\n" +
      "D;JEQ\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=0\n" +
      "@EQ.after." + n + "\n" +
      "0;JMP\n" +
      "(EQ.true." + n + ")\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=-1\n" +
      "(EQ.after." + n + ")\n";
    return s;
  }

  private String GT() {
    String n = nextCount();
    String s =
      "@SP\n" +
      "AM=M-1\n" +
      "D=M\n" +
      "A=A-1\n" +
      "D=M-D\n" +
      "@GT.true." + n + "\n" +
      "\nD;JGT\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=0\n" +
      "@GT.after." + n + "\n" +
      "0;JMP\n" +
      "(GT.true." + n + ")\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=-1\n" +
      "(GT.after." + n + ")\n";
    return s;
  }

  private String LT() {
    String n = nextCount();
    String s =
      "@SP\n" +
      "AM=M-1\n" +
      "D=M\n" +
      "A=A-1\n" +
      "D=M-D\n" +
      "@LT.true." + n + "\n" +
      "D;JLT\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=0\n" +
      "@LT.after." + n + "\n" +
      "0;JMP\n" +
      "(LT.true." + n + ")\n" +
      "@SP\n" +
      "A=M-1\n" +
      "M=-1\n" +
      "(LT.after." + n + ")\n";
    return s;
  }

  private String GOTO(String label) {
    String s =
      "@" + funcName + "$" + label + "\n" +
      "0;JMP\n";
    return s;
  }

  private String IFGOTO(String label) {
    String s =
      "@SP\n" +
      "AM=M-1\n" +
      "D=M\n" +
      "@" + funcName + "$" + label + "\n" +
      "D;JNE\n";
    return s;
  }

  private String FUNCTION(String f, String k) {
    String s =
      "(" + f + ")\n" +
      "@SP\n" +
      "A=M\n";
    int kk = Integer.parseInt(k);
    for (int i = 0; i < kk; i += 1) {
      s +=
        "M=0\n" +
        "A=A+1\n";
    }
    return s +
      "D=A\n" +
      "@SP\n" +
      "M=D\n";
  }

  private String CALL(String f, String n) {
    String c = nextCount();
    return
      // SP -> R13
      "@SP\n" +
      "D=M\n" +
      "@R13\n" +
      "M=D\n" +
      // @RET -> *SP
      "@RET." + c + "\n" +
      "D=A\n" +
      "@SP\n" +
      "A=M\n" +
      "M=D\n" +
      // SP++
      "@SP\n" +
      "M=M+1\n" +
      // LCL -> *SP
      "@LCL\n" +
      "D=M\n" +
      "@SP\n" +
      "A=M\n" +
      "M=D\n" +
      // SP++
      "@SP\n" +
      "M=M+1\n" +
      // ARG -> *SP
      "@ARG\n" +
      "D=M\n" +
      "@SP\n" +
      "A=M\n" +
      "M=D\n" +
      // SP++
      "@SP\n" +
      "M=M+1\n" +
      // THIS -> *SP
      "@THIS\n" +
      "D=M\n" +
      "@SP\n" +
      "A=M\n" +
      "M=D\n" +
      // SP++
      "@SP\n" +
      "M=M+1\n" +
      // THAT -> *SP
      "@THAT\n" +
      "D=M\n" +
      "@SP\n" +
      "A=M\n" +
      "M=D\n" +
      // SP++
      "@SP\n" +
      "M=M+1\n" +
      // R13 - n -> ARG
      "@R13\n" +
      "D=M\n" +
      "@" + n + "\n" +
      "D=D-A\n" +
      "@ARG\n" +
      "M=D\n" +
      // SP -> LCL
      "@SP\n" +
      "D=M\n" +
      "@LCL\n" +
      "M=D\n" +
      "@" + f + "\n" +
      "0;JMP\n" +
      "(RET." + c + ")\n";
  }

  public static void main(String files[]) {
    if (files.length == 0) {
      System.err.println("no input files");
      return;
    }
    Translator p = new Translator(files);
    String init =
      "@256\n" +
      "D=A\n" +
      "@SP\n" +
      "M=D\n" +
      "// call Sys.init 0\n" +
      p.CALL("Sys.init", "0") +
      "0;JMP\n";
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
