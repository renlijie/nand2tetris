import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Parse assembly code (.asm) to machine binary code.
class Parser {
  private static final char A_COMMAND = 0;
  private static final char C_COMMAND = 1;
  private static final char L_COMMAND = 2;

  private Map<String, Integer> symbols = new HashMap<String, Integer>(50);
  private BufferedReader br;
  private int currentSymbolAddress = 16;
  private int currentCodeLine = 0;

  public Parser(String file) {
		try {
			br = new BufferedReader(new FileReader(file));
      symbols.put("SP", 0);
      symbols.put("LCL", 1);
      symbols.put("ARG", 2);
      symbols.put("THIS", 3);
      symbols.put("THAT", 4);
      symbols.put("SCREEN", 16384);
      symbols.put("KBD", 24576);
      symbols.put("R0", 0);
      symbols.put("R1", 1);
      symbols.put("R2", 2);
      symbols.put("R3", 3);
      symbols.put("R4", 4);
      symbols.put("R5", 5);
      symbols.put("R6", 6);
      symbols.put("R7", 7);
      symbols.put("R8", 8);
      symbols.put("R9", 9);
      symbols.put("R10", 10);
      symbols.put("R11", 11);
      symbols.put("R12", 12);
      symbols.put("R13", 13);
      symbols.put("R14", 14);
      symbols.put("R15", 15);

      firstPass();
      close();
      br = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
    return;
  }

  private void firstPass() throws Exception {
    String s = nextCommand();
    while(s != null) {
      if (commandType(s) == L_COMMAND) {
        if (symbols.put(s.substring(1, s.length() - 1), currentCodeLine) != null)
          throw new Exception("symbol " + s.substring(1, s.length() - 1) + " defined multiple times!");
      } else
        currentCodeLine += 1;
      s = nextCommand();
    }
    return;
  }

  private String nextCommand() throws IOException {
    String line;
    while(true) {
      line = br.readLine();
      if (line == null) {
        close();
        return null;
      }
      line = line.replaceAll("\\s","");
      line = line.replaceAll("//.*", "");
      if (line.length() == 0)
        continue;
      return line;
    }
  }

  private char commandType(String command) {
    if (command.charAt(0) == '@')
      return A_COMMAND;
    if (command.charAt(0) == '(')
      return L_COMMAND;
    return C_COMMAND;
  }

  // 3 bit: toA, toD, toM
  private char dest(String command) {
    if (command.indexOf('=') == -1)
      return 0;
    String lhs = command.replaceAll("=.*", "");
    char res = 0;
    if (lhs.indexOf('A') != -1)
      res |= 4;
    if (lhs.indexOf('D') != -1)
      res |= 2;
    if (lhs.indexOf('M') != -1)
      res |= 1;
    return res;
  }

  private char comp(String command) throws Exception {
    String s = command.replaceAll(".*=", "");
    s = s.replaceAll(";.*", "");
    switch (s) {
      case "0": return 0b0101010;
      case "1": return 0b0111111;
      case "-1": return 0b0111010;
      case "D": return 0b0001100;
      case "A": return 0b0110000;
      case "!D": return 0b0001101;
      case "!A": return 0b0110001;
      case "-D": return 0b0001111;
      case "-A": return 0b0110011;
      case "D+1": return 0b0011111;
      case "A+1": return 0b0110111;
      case "D-1": return 0b0001110;
      case "A-1": return 0b0110010;
      case "D+A": return 0b0000010;
      case "D-A": return 0b0010011;
      case "A-D": return 0b0000111;
      case "D&A": return 0b0000000;
      case "D|A": return 0b0010101;

      case "M": return 0b1110000;
      case "!M": return 0b1110001;
      case "-M": return 0b1110011;
      case "M+1": return 0b1110111;
      case "M-1": return 0b1110010;
      case "D+M": return 0b1000010;
      case "D-M": return 0b1010011;
      case "M-D": return 0b1000111;
      case "D&M": return 0b1000000;
      case "D|M": return 0b1010101;
      default: throw new Exception("not valid comp!");
    }
  }

  private char jump(String command) {
    if (command.indexOf(';') == -1)
      return 0;
    String rhs = command.replaceAll(".*;", "");
    switch (rhs) {
      case "JGT": return 0b001;
      case "JEQ": return 0b010;
      case "JGE": return 0b011;
      case "JLT": return 0b100;
      case "JNE": return 0b101;
      case "JLE": return 0b110;
      case "JMP": return 0b111;
      default: return 0;
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

  public String parseNextCommand() throws Exception {
    String s = nextCommand();
    while(s != null && commandType(s) == L_COMMAND)
      s = nextCommand();
    if (s == null)
      return null;
    if (commandType(s) == A_COMMAND) {
      s = s.substring(1);
      if (s.charAt(0) < '0' || s.charAt(0) > '9') {
        Integer addr = (Integer) symbols.get(s);
        if (addr == null) {
          addr = currentSymbolAddress;
          symbols.put(s, addr);
          currentSymbolAddress += 1;
        }
        return String.format("%16s", Integer.toBinaryString(addr)).replace(' ', '0');
      } else
        return String.format("%16s", Integer.toBinaryString(Integer.parseInt(s))).replace(' ', '0');
    }
    int raw = 0b1110000000000000 + (comp(s) << 6) + (dest(s) << 3) + jump(s);
    return String.format("%16s", Integer.toBinaryString(raw)).replace(' ', '0');
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      System.err.println("no input file");
      return;
    }
    Parser p = new Parser(args[0]);
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
