package com.lewisk.emulation;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

// Takes a file, reads it then returns byte-code.
public class Assemble6502
{
    protected static class InstrData
    {
        String outputData;
        String name;
        String args;
        ModeData mode;
        Integer[] bytes;

        protected InstrData(String name, String args, Integer[] bytes)
        {
            this.name = name;
            this.args = args;
            this.bytes = bytes;
        }
    }

    protected static class ModeData
    {
        String regex;
        int byteCount;
        int ID;

        protected ModeData(String regex, int byteCount, int ID)
        {
            this.regex = regex;
            this.byteCount = byteCount;
            this.ID = ID;
        }
    }

    protected static int getOpcode(InstrData instr)
    {
        Map<String, String> opcodes = new HashMap<>();
        // ACC, IMP, IMM, ZP, ZPX, ZPY, ABS, ABX, ABY, IN, INX, INY
        // ZeroPage = Relative in Branching instructions.
        opcodes.put("ADC", "FF FF 69 65 75 FF 6D 7D 79 FF 61 71");
        opcodes.put("AND", "FF FF 29 25 35 FF 2D 3D 39 FF 21 31");
        opcodes.put("ASL", "0A 0A FF 06 16 FF 0E 1E FF FF FF FF");
        opcodes.put("BCC", "FF FF FF 90 FF FF FF FF FF FF FF FF");
        opcodes.put("BCS", "FF FF FF B0 FF FF FF FF FF FF FF FF");
        opcodes.put("BEQ", "FF FF FF F0 FF FF FF FF FF FF FF FF");
        opcodes.put("BIT", "FF FF FF 24 FF FF 2C FF FF FF FF FF");
        opcodes.put("BMI", "FF FF FF 30 FF FF FF FF FF FF FF FF");
        opcodes.put("BNE", "FF FF FF D0 FF FF FF FF FF FF FF FF");
        opcodes.put("BPL", "FF FF FF 10 FF FF FF FF FF FF FF FF");
        opcodes.put("BRK", "FF 00 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("BVC", "FF FF FF 50 FF FF FF FF FF FF FF FF");
        opcodes.put("BVS", "FF FF FF 70 FF FF FF FF FF FF FF FF");
        opcodes.put("CLC", "FF 18 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("CLD", "FF D8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("CLI", "FF 58 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("CLV", "FF B8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("CMP", "FF FF C9 C5 D5 FF CD DD D9 FF C1 D1");
        opcodes.put("CPX", "FF FF E0 E4 FF FF EC FF FF FF FF FF");
        opcodes.put("CPY", "FF FF C0 C4 FF FF CC FF FF FF FF FF");
        opcodes.put("DEC", "FF FF FF C6 D6 FF CE DE FF FF FF FF");
        opcodes.put("DEX", "FF CA FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("DEY", "FF 88 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("EOR", "FF FF 49 45 55 FF 4D 5D 59 FF 41 51");
        opcodes.put("INC", "FF FF FF E6 F6 FF EE FE FF FF FF FF");
        opcodes.put("INX", "FF E8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("INY", "FF C8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("JMP", "FF FF FF FF FF FF 4C FF FF 6C FF FF");
        opcodes.put("JSR", "FF FF FF FF FF FF 20 FF FF FF FF FF");
        opcodes.put("LDA", "FF FF A9 A5 B5 FF AD BD B9 FF A1 B1");
        opcodes.put("LDX", "FF FF A2 A6 FF B6 AE FF BE FF FF FF");
        opcodes.put("LDY", "FF FF A0 A4 B4 FF AC BC FF FF FF FF");
        opcodes.put("LSR", "4A 4A FF 4A 56 FF 4E 5E FF FF FF FF");
        opcodes.put("NOP", "FF EA FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("ORA", "FF FF 09 05 15 FF 0D 1D 19 FF 01 11");
        opcodes.put("PHA", "FF 48 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("PHP", "FF 08 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("PLA", "FF 68 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("PLP", "FF 28 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("ROL", "2A 2A FF 26 36 FF 2E 3E FF FF FF FF");
        opcodes.put("ROR", "6A 6A FF 66 76 FF 6E 7E FF FF FF FF");
        opcodes.put("RTI", "FF 40 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("RTS", "FF 60 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("SBC", "FF FF E9 E5 F5 FF ED FD F9 FF E1 F1");
        opcodes.put("SEC", "FF 38 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("SED", "FF F8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("SEI", "FF 78 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("STA", "FF FF FF 85 95 FF 8D 9D 99 FF 81 91");
        opcodes.put("STX", "FF FF FF 86 FF 96 8E FF FF FF FF FF");
        opcodes.put("STY", "FF FF FF 84 94 FF 8C FF FF FF FF FF");
        opcodes.put("TAX", "FF AA FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("TAY", "FF A8 FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("TSX", "FF BA FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("TXA", "FF 8A FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("TXS", "FF 9A FF FF FF FF FF FF FF FF FF FF");
        opcodes.put("TYA", "FF 98 FF FF FF FF FF FF FF FF FF FF");

        String instrCodes = opcodes.get(instr.name.toUpperCase());
        if(instrCodes == null)
        {
            System.out.format("Invalid name: %s\n", instr.name);
            return 0;
        }
        String[] codes = instrCodes.split(" ");
        String finalCode = codes[instr.mode.ID];
        if(finalCode.equals("FF"))
        {
            System.out.format("No such instruction: %s, %d\n", instr.name, instr.mode.ID);
            return 0;
        }
        return Integer.parseInt(finalCode, 16);
    }

    public static InstrData getBytes(String src)
    {
        // Assembler Data
        Map<String, ModeData> modesRegex = new HashMap<>();
        modesRegex.put("ACC", new ModeData("^a$",                                0, 0));
        modesRegex.put("IMP", new ModeData("^(?!.)",                             0, 1));
        modesRegex.put("IMMD", new ModeData("^\\#[0123456789abcdef]{1,3}",       1, 2));
        modesRegex.put("IMM", new ModeData("^\\Q#$\\E[0123456789abcdef]{2}",     1, 2));
        modesRegex.put("ZP",  new ModeData("^\\$[0123456789abcdef]{2}",          1, 3));
        modesRegex.put("ZPX", new ModeData("^\\$[0123456789abcdef]{2},x",        1, 4));
        modesRegex.put("ZPY", new ModeData("^\\$[0123456789abcdef]{2},y",        1, 5));
        modesRegex.put("ABS", new ModeData("^\\$[0123456789abcdef]{4}",          2, 6));
        modesRegex.put("ABX", new ModeData("^\\$[0123456789abcdef]{4},x",        2, 7));
        modesRegex.put("ABY", new ModeData("^\\$[0123456789abcdef]{4},y",        2, 8));
        modesRegex.put("IN",  new ModeData("^\\(\\$[0123456789abcdef]{4}\\)$",   2, 9));
        modesRegex.put("INX", new ModeData("^\\(\\$[0123456789abcdef]{2},x\\)$", 1, 10));
        modesRegex.put("INY", new ModeData("^\\(\\$[0123456789abcdef]{2}\\),y$", 1, 11));

        // Bytes
        ArrayList<Integer> output = new ArrayList<>();
        var instrOutput = new InstrData("","", null);

        // Filter whitespace
        String code = src.replaceAll("\\s","");

        if(code.length() < 3) return null;

        // Get instruction and arguments.
        String instr = code.substring(0,3);
        String args  = code.substring(3);
        args = args.toLowerCase();

        // Resolve instruction mode.
        ModeData mode = null;
        String modeName = "NUL";
        int instrBytes = 1;

        for(Map.Entry<String, ModeData> e : modesRegex.entrySet())
        {
            var modeData = e.getValue();
            if(args.matches(modeData.regex))
            {
                instrBytes += modeData.byteCount;
                mode = modeData;
                modeName = e.getKey();
                break;
            }
        }

        instrOutput.name = instr;
        instrOutput.args = args;
        if(mode == null)
        {
            return instrOutput;
        }
        instrOutput.mode = mode;

        StringBuilder outputInfo = new StringBuilder();
        outputInfo.append(String.format("%s %s\t[%d] -> ", instr.toUpperCase(), modeName, instrBytes));

        output.add(getOpcode(instrOutput)); // TODO: Properly resolve instruction opcode data.
        outputInfo.append(String.format("%02x ", output.get(0)));
        if(instrBytes > 1)
        {
            int argStart = args.indexOf("$") + 1;
            int argStartDec = args.indexOf("#") + 1;
            if(argStart > 0)
            {
                String argsData = args.substring(argStart, argStart + ((instrBytes - 1) * 2));
                String[] argsHex = argsData.split("(?<=\\G..)");
                for (int i = argsHex.length - 1; i >= 0; i--) {
                    String hex = argsHex[i];
                    if (hex.equals("")) continue;
                    int hexValue = Integer.parseInt(hex, 16);
                    output.add(hexValue);
                    outputInfo.append((String.format("%02x ", hexValue)));
                }
            }
            else if(argStartDec > 0)
            {
                String argsData = args.substring(argStartDec);
                int decValue = Integer.parseInt(argsData, 10);
                if(decValue > 255) decValue = 255;
                output.add(decValue);
                outputInfo.append((String.format("%02x ", decValue)));
            }
        }

        outputInfo.append("\n");
        Integer[] bytes = new Integer[output.size()];
        bytes = output.toArray(bytes);
        instrOutput.bytes = bytes;
        instrOutput.outputData = outputInfo.toString();

        return instrOutput;
    }

    public static String[] assemble(String file)
    {
        ArrayList<String> inputLines = new ArrayList<>();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream fileIO = classloader.getResourceAsStream(file);
        Scanner scan = new Scanner(fileIO);

        // Read lines
        while(scan.hasNextLine()) inputLines.add(scan.nextLine());

        String[] data = new String[inputLines.size()];
        inputLines.toArray(data);
        return assemble(data);
    }

    public static String[] assemble(String[] data)
    {
        ArrayList<Integer> outputHex = new ArrayList<>();

        // Filter data
        ArrayList<String> fData = new ArrayList<>();
        for(String line : data)
        {
            // Filter outer whitespace.
            line = line.trim();

            // Filter empty strings
            if(line.length() < 3) continue;

            // Filter comments
            int commentIndex = line.indexOf(";");
            if(commentIndex == 0) continue;
            if(commentIndex >= 0) line = line.substring(0, commentIndex);

            // Finalize
            fData.add(line);
        }

        int entryPoint = 0x0600;
        Map<String, String> asmVars = new HashMap<>();
        Map<String, Integer> asmLabels = new HashMap<>();

        // Labels, Variables (Also filters out)
        for(int i = fData.size()-1; i >= 0; i--)
        {
            String line = fData.get(i);

            // Determine entry point.
            if(line.matches("^\\.entry \\$.+"))
            {
                String[] varArgs = line.split("\\$");
                if(varArgs.length != 2)
                {
                    System.out.format("Syntax error -> %s\n", line);
                    return null;
                }
                entryPoint = Integer.parseInt(varArgs[1], 16);
                System.out.format("@entry %04x\n", entryPoint);
                fData.remove(i);
            }

            // Find variables
            if(line.matches("^define.+"))
            {
                String[] varArgs = line.split("[^_a-zA-Z\\$0-9]+");
                if(varArgs.length != 3)
                {
                    System.out.format("Syntax error -> %s\n", line);
                    return null;
                }
                asmVars.put(varArgs[1],varArgs[2]);
                fData.remove(i);
            }
            // Find Labels
            else if(line.contains(":"))
            {
                int labelEnd = line.indexOf(":");
                String name = line.substring(0, labelEnd);
                if(name.contains(" "))
                {
                    System.out.format("Syntax error -> %s\nLabel names cannot contain spaces.", line);
                    return null;
                }
                asmLabels.put(name, 0);
            }
        }

        // Replace relevant data with valid assembler.
        int byteCount = 0;
        for(int i = 0; i < fData.size(); i++)
        {
            String line = fData.get(i);

            // Replace variables
            for(Map.Entry<String, String> e : asmVars.entrySet())
            {
                String search = ".*\\b" + e.getKey() + "\\b.*";
                if(line.matches(search))
                {
                   line = line.replace(e.getKey(), e.getValue());
                   fData.set(i,line);
                }
            }

            // Define labels
            if(line.contains(":"))
            {
                int labelEnd = line.indexOf(":");
                String name = line.substring(0, labelEnd);
                asmLabels.replace(name, byteCount);
                line = line.replace(name + ":", "");
                String labelTest = line.replaceAll("\\s", "");
                if(labelTest.length() == 0) continue;
            }

            // Replace Labels (With temporary value)
            for(Map.Entry<String, Integer> e : asmLabels.entrySet())
            {
                String search = ".*\\b" + e.getKey() + "\\b.*";
                if(line.matches(search))
                {
                    var bytecode = getBytes(line);
                    String addr = "$00";
                    if(bytecode.name.equalsIgnoreCase("JMP") || bytecode.name.equalsIgnoreCase("JSR"))
                    {
                        addr = "$0000";
                    }
                    line = line.replace(e.getKey(), addr);
                }
            }

            // Count bytes
            var hexData = getBytes(line);
            if(hexData.bytes == null)
            {
                System.out.format("Syntax error -> %s\n", line);
                return null;
            }

            byteCount += hexData.bytes.length;
        }

        // To Bytecode
        byteCount = 0;
        for(String line : fData)
        {
            // Skip labels
            if(line.contains(":"))
            {
                int labelEnd = line.indexOf(":");
                String name = line.substring(0, labelEnd);
                line = line.replace(name + ":", "");
                String labelTest = line.replaceAll("\\s", "");
                if(labelTest.length() == 0) continue;
            }

            // Replace labels
            for(Map.Entry<String, Integer> e : asmLabels.entrySet())
            {
                String search = ".*\\b" + e.getKey() + "\\b.*";
                if(line.matches(search))
                {
                    // Assemble instruction to determine if JMP or Branch.
                    var bytecode = getBytes(line);
                    String addr = "";

                    if(bytecode.name.equalsIgnoreCase("JMP") || bytecode.name.equalsIgnoreCase("JSR"))
                    {
                        addr = String.format("%04x",entryPoint+e.getValue());
                    }
                    else
                    {
                        int offs = e.getValue() - (byteCount + 2);
                        if(offs > 128 || offs < -127)
                        {
                            System.out.format("Branch out of range -> %d [Distance: %d]", line, offs);
                            return null;
                        }
                        addr = String.format("%02x",Byte.toUnsignedInt((byte) offs));
                    }

                    line = line.replace(e.getKey(), "$" + addr);
                }
            }

            var bytecode = getBytes(line);
            if(bytecode.bytes == null)
            {
                System.out.format("Syntax error -> %s\n", line);
                return null;
            }

            outputHex.addAll(Arrays.asList(bytecode.bytes));

            byteCount += bytecode.bytes.length;
        }

        ArrayList<String> disassembly = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for(int i = entryPoint; i < entryPoint+outputHex.size(); i++)
        {
            if( (i & 0x0F) == 0 )
            {
                if(line.toString().length() > 0)
                {
                    disassembly.add(line.toString());
                    line.setLength(0); // Clear line.
                }
                line.append(String.format("%04x: ", i));
            }
            line.append(String.format("%02x ", outputHex.get(i-entryPoint)));
        }
        disassembly.add(line.toString());

        System.out.println();
        for(String s : disassembly)
        {
            System.out.println(s);
        }

        String[] asm = new String[disassembly.size()];
        asm = disassembly.toArray(asm);
        return asm;
    }
}
