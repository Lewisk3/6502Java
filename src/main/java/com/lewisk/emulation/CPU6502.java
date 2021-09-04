package com.lewisk.emulation;

import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

// Runs bytecode on 6502 VM also hosts memory and registers
// on virtual CPU.
public class CPU6502 implements Runnable
{
    // Define addressable memory.
    public static final int MEMSIZE = 0x10000;
    private byte[] ram;
    public double clockspeed;
    protected int waitCycles;
    protected long clock;

    // IO
    public PixelWriter display;

    // Colors
    public static final Color[] colortable =
    {
            Color.web("0x000000", 1.0),
            Color.web("0xFFFFFF", 1.0),
            Color.web("0x880000", 1.0),
            Color.web("0xAAFFEE", 1.0),
            Color.web("0xCC44CC", 1.0),
            Color.web("0x00CC55", 1.0),
            Color.web("0x0000AA", 1.0),
            Color.web("0xEEEE77", 1.0),
            Color.web("0xDD8855", 1.0),
            Color.web("0x664400", 1.0),
            Color.web("0xFF7777", 1.0),
            Color.web("0x333333", 1.0),
            Color.web("0x777777", 1.0),
            Color.web("0xAAFF66", 1.0),
            Color.web("0x0088FF", 1.0),
            Color.web("0xBBBBBB", 1.0)
    };

    // Flags
    // This is what I do not like about Java so far.
    // I wish more traditional enums where an option,
    // and they didn't have to be a class...
    public enum Flags
    {
        Carry            (1 << 0),
        Zero             (1 << 1),
        InterruptDisable (1 << 2),
        DecimalMode      (1 << 3),
        Break            (1 << 4),
        Undefined        (1 << 5),
        Overflow         (1 << 6),
        Negative         (1 << 7);

        private int value;
        private Flags(int val)
        {
            this.value = val;
        }
    };
    protected byte cpuFlags;

    // CPU registers.
    protected static class CpuRegisters
    {
        short PC; // Program Counter
        byte  SP; // Stack Pointer

        // Accumulator, Index X, Index Y
        byte A,X,Y;
    }
    CpuRegisters registers;

    // How many cycles CPU has been running for
    public int executiontime;

    // True when execution is to be stopped.
    public boolean halt;

    public CPU6502()
    {
        // Initialize
        ram = new byte[MEMSIZE];
        registers = new CpuRegisters();
        registers.SP = (byte) 0xFF;
        registers.PC = 0x0600;
        registers.A = registers.X = registers.Y = 0;
        executiontime = 0;
        halt = true;
        clockspeed = 50000; //3e+6; // 3Mhz
        resetFlags();
    }

    // Flags
    public boolean getFlag(Flags flag)
    {
        return (cpuFlags & flag.value) != 0;
    }

    protected void setFlag(Flags flag, boolean enable)
    {
        if(enable)
            cpuFlags |= flag.value;
        else
            cpuFlags &= ~flag.value;
    }

    protected void resetFlags()
    {
        cpuFlags = 0;
        setFlag(Flags.Undefined, true);
        setFlag(Flags.Break, true);
    }

    // RAM
    public byte readRAM(short address)
    {
        return ram[Short.toUnsignedInt(address)];
    }

    public void writeRAM(short address, int value)
    {
        ram[Short.toUnsignedInt(address)] = (byte) value;

        // Update display
        if(address >= 0x0200 && address <= 0x5ff) DrawVRAM(address);
    }

    protected void writeRAM_WithFlags(short address, int value)
    {
        writeRAM(address, value);
        setFlagsResult((byte) value);
    }

    public byte nextBytePC()
    {
        return readRAM(++registers.PC);
    }

    public short nextBytesPC()
    {
        int L = Byte.toUnsignedInt(nextBytePC());
        int H = Byte.toUnsignedInt(nextBytePC());

        return bytesToShort(L, H);
    }

    public short bytesToShort(int Low, int High)
    {
        return (short) ((High << 8) | Low);
    }

    public void importRAM(short from, int... mem) { for(int b: mem) writeRAM(from++, b); }
    public void importRAM(String file)
    {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream fileIO = classloader.getResourceAsStream(file);
        Scanner scan = new Scanner(fileIO);

        // Read lines
        ArrayList<String> data = new ArrayList<>();
        while(scan.hasNextLine()) data.add(scan.nextLine());
        String[] memData = new String[data.size()];
        memData = data.toArray(memData);

        importRAM(memData);
    }

    public void importRAM(String[] memData)
    {
        if(memData == null)
        {
            System.out.println("No data to import.");
            return;
        }

        // Read lines, Begin import
        for (String line : memData)
        {
            String[] writeData;
            String[] codeData;
            writeData = line.split(":");
            codeData = writeData[1].split(" ");

            int address = Integer.parseInt(writeData[0], 16);
            for(String hex : codeData)
            {
                if(hex.equals("")) continue;
                int hexdata = Integer.parseInt(hex, 16);

                // Write to RAM
                writeRAM((short) address++, hexdata);
            }
        }
    }


    // Memory addressing
    protected short immediateAddress()
    {
        return (short) Byte.toUnsignedInt(nextBytePC());
    }
    protected short zeroPage(byte reg)
    {
        byte zp = (byte) (nextBytePC() + reg);
        return (short) Byte.toUnsignedInt(zp);
    }
    protected short absolute(byte reg)
    {
        return (short) (nextBytesPC() + Byte.toUnsignedInt(reg));
    }
    protected short indirectX()
    {
        short zp = (short) (nextBytePC() + registers.X);
        zp = (short) Byte.toUnsignedInt((byte) zp);
        int L = Byte.toUnsignedInt(readRAM(zp));
        int H = Byte.toUnsignedInt(readRAM(++zp));

        return bytesToShort(L, H);
    }
    protected short indirectY()
    {
        short zp = (short) Byte.toUnsignedInt(nextBytePC());
        int L = Byte.toUnsignedInt(readRAM(zp));
        int H = Byte.toUnsignedInt(readRAM(++zp));

        return (short) (bytesToShort(L, H) + registers.Y);
    }

    protected byte readZeroPage(byte reg)
    {
        return readRAM(zeroPage(reg));
    }
    protected byte readAbsolute(byte reg)
    {
        return readRAM(absolute(reg));
    }
    protected short zeroPage() { return (short) Byte.toUnsignedInt(nextBytePC()); }
    protected short absolute() { return nextBytesPC(); }
    protected byte readZeroPage()
    {
        return readRAM(zeroPage());
    }
    protected byte readAbsolute() { return readRAM(absolute()); }
    protected byte readIndirectX()
    {
        return readRAM(indirectX());
    }
    protected byte readIndirectY()
    {
        return readRAM(indirectX());
    }

    // Stack
    protected void stackPush(int value)
    {
        writeRAM((short) ( 0x100 + Byte.toUnsignedInt(registers.SP--) ), value);
    }

    protected byte stackPop()
    {
        return readRAM((short) ( 0x100 + Byte.toUnsignedInt(++registers.SP) ));
    }

    protected void pushPC(int offs)
    {
        short toaddr = (short) (registers.PC + offs);

        stackPush(toaddr >> 8);
        stackPush((toaddr & 0xFF));
    }

    protected void popPC()
    {
        int L = Byte.toUnsignedInt(stackPop());
        int H = Byte.toUnsignedInt(stackPop());
        registers.PC = bytesToShort(L,H);
    }

    // Update Flags
    protected void setFlagsCMP(byte A, byte B)
    {
        int cmp = Integer.compare(Byte.toUnsignedInt(A), Byte.toUnsignedInt(B));
        byte R = (byte) (A-B);
        setFlag(Flags.Carry, cmp >= 0);
        setFlagsResult(R);
    }

    protected void setFlagsADC(byte A, byte B)
    {
        int carryR = Byte.toUnsignedInt(A) + Byte.toUnsignedInt(B);
        int intR = (int)A + (int)B;

        if(carryR > 255) setFlag(Flags.Carry, true);
        setFlagsResult((byte) carryR, intR);
    }

    protected void setFlagsSBC(byte A, byte B)
    {
        int carryR = Byte.toUnsignedInt(A) - Byte.toUnsignedInt(B);
        int intR = (int)A - (int)B;

        if(carryR < 0) setFlag(Flags.Carry, false);
        setFlagsResult((byte) carryR, intR);
    }

    protected void setFlagsResult(byte byteResult)
    {
        setFlag(Flags.Zero, byteResult == 0);
        setFlag(Flags.Negative, byteResult < 0);

    }
    protected void setFlagsResult(byte byteResult, int intResult)
    {
        setFlagsResult(byteResult);
        setFlag(Flags.Overflow, intResult > 128 || intResult < -127);
    }

    // Registers
    protected void setRegA(byte value)
    {
        registers.A = value;
        setFlagsResult(registers.A);
    }
    protected void setRegX(byte value)
    {
        registers.X = value;
        setFlagsResult(registers.X);
    }
    protected void setRegY(byte value)
    {
        registers.Y = value;
        setFlagsResult(registers.Y);
    }

    // Math utility
    public static double hertzToNanoseconds(double hz)
    {
        return (1.0 / hz) * 1e+9;
    }

    // Output
    public String getCPUInfo()
    {
        String reg_status = String.format("A=$%02x X=$%02x Y=$%02x", registers.A, registers.X, registers.Y);
        String ptr_status = String.format("SP=$%02x PC=$%04x", registers.SP, registers.PC);
        StringBuilder flag_bits = new StringBuilder();

        for (Flags f : Flags.values()) flag_bits.append(getFlag(f) ? '1' : '0');
        String flag_status = "NV-BDIZC\n" + flag_bits.reverse().toString();

        return reg_status + "\n" + ptr_status + "\n" + flag_status + "\n";
    }

    protected int executeInstruction(byte opcode)
    {
        var instruction = Opcode6502.getInstruction(opcode);
        int cycles = 0;
        try
        {
            cycles = instruction.execute(this);
        }
        catch (Exception e)
        {
            System.out.format("Encountered critical error executing instruction: %02x\nExecution halted at: %04x \n >> %s", opcode, registers.PC, e.toString());
            halt = true;
        }
        registers.PC++;

        return cycles;
    }

    public void stepPrompt()
    {
        System.out.println("Press \"ENTER\" to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public void printRAM(int from, int len)
    {
        for(int i = from; i < from+len; i++)
        {
            if( (i & 0x0F) == 0 )
                System.out.format("\n%04x: ", i);

            System.out.format("%02x ", readRAM((short) i));
        }
        System.out.println();
    }

    public void DrawVRAM()
    {
        if(display == null) return;
        int x, y;
        x = y = 0;
        for (short i = 0x0200; i < 0x0600; i++)
        {
            var pixel = readRAM(i) & 0x0F;
            display.setColor(x, y, colortable[pixel]);
            if (++x > 31)
            {
                x = 0;
                y++;
            }
        }
    }

    public void DrawVRAM(short addr)
    {
        if(display == null) return;
        int offset = addr - 0x0200;
        int x = offset % 32;
        int y = offset / 32;

        var pixel = readRAM(addr) & 0x0F;
        display.setColor(x, y, colortable[pixel]);
    }

    // Start execution
    public void execute()
    {
        // Read from PC and execute instruction.
        byte opcode = readRAM(registers.PC);
        int cycles = executeInstruction(opcode);
        executiontime += cycles;
        waitCycles = cycles;
    }

    // Start execution
    @Override
    public void run()
    {
        if(halt)
        {
            executiontime = 0;
            halt = false;
        }

        Random rng = new Random();
        while(!halt)
        {
            if(Thread.interrupted())
            {
                halt = true;
                break;
            }

            // Wait until next clock signal.
            if( (System.nanoTime() - clock) > hertzToNanoseconds(clockspeed))
            {
                clock = System.nanoTime();
                waitCycles--;

                // Get random number
                writeRAM((short) 0x00FE,rng.nextInt() & 0xFF);
            }

            // Only run if all cycles have been ran.
            if(waitCycles <= 0)
            {
                execute();
            }
        }
    }
}
