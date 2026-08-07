class CPU {
    void process() {
        System.out.println("CPU is processing data");
    }
}

class RAM {
    void load() {
        System.out.println("RAM is loading data");
    }
}

class HardDrive {
    void read() {
        System.out.println("HardDrive is reading data");
    }
}

public class Computer {
    private CPU cpu = new CPU();
    private RAM ram = new RAM();
    private HardDrive hd = new HardDrive();

    void turnOn() {
        cpu.process();
        ram.load();
        hd.read();
        System.out.println("Computer is ready");
    }

    void turnOff() {
        System.out.println("Computer shutting down");
    }
    public static void main(String[] args) {
        Computer pc = new Computer();
        pc.turnOn();
        pc.turnOff();
    }
}
