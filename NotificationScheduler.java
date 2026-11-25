import java.util.*;
import java.io.*;

class Notification {
    String id;
    String time;
    String message;
    int repeatInterval;

    Notification(String id, String time, String message, int repeatInterval) {
        this.id = id;
        this.time = time;
        this.message = message;
        this.repeatInterval = repeatInterval;
    }

    int toMinutes() {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60
                + Integer.parseInt(parts[1]);
    }
}

class CircularQueue {
    private Notification[] arr;
    private int front, rear, size, capacity;

    CircularQueue(int capacity) {
        this.capacity = capacity;
        arr = new Notification[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    boolean isEmpty() {
        return size == 0;
    }

    boolean enqueue(Notification n) {
        if (size == capacity)
            return false;

        rear = (rear + 1) % capacity;
        arr[rear] = n;
        size++;
        return true;
    }

    Notification dequeue() {
        if (isEmpty())
            return null;

        Notification n = arr[front];
        front = (front + 1) % capacity;
        size--;
        return n;
    }

    void remove(String id) {
        Notification[] temp = new Notification[capacity];
        int count = 0;

        while (!isEmpty()) {
            Notification n = dequeue();
            if (!n.id.equals(id))
                temp[count++] = n;
        }

        for (int i = 0; i < count; i++)
            enqueue(temp[i]);
    }

    void display() {
        int i = front;

        for (int count = 0; count < size; count++) {
            Notification n = arr[i];
            System.out.println(n.id + " | " + n.time
                    + " | " + n.message
                    + " | Every " + n.repeatInterval + " minutes");
            i = (i + 1) % capacity;
        }
    }
}

public class NotificationScheduler {

    static PriorityQueue<Notification> pq =
        new PriorityQueue<>((a, b) -> {
            int cmp = Integer.compare(a.toMinutes(), b.toMinutes());
            return cmp != 0 ? cmp : a.id.compareTo(b.id);
        });

    static Queue<Notification> readyQueue = new LinkedList<>();
    static CircularQueue recurringQueue = new CircularQueue(100);

    static HashSet<String> cancelled = new HashSet<>();

    static int nextId = 101;
    static final String FILE_NAME = "notifications.txt";

    static boolean validTime(String time) {
        if (!time.matches("\\d{2}:\\d{2}"))
            return false;

        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return hour >= 0 && hour <= 23
                && minute >= 0 && minute <= 59;
    }

    static boolean idExists(String id) {
        for (Notification n : pq)
            if (n.id.equals(id))
                return true;

        for (Notification n : readyQueue)
            if (n.id.equals(id))
                return true;

        return false;
    }

    static void schedule(String time, String message, int interval) {
        if (!validTime(time)) {
            System.out.println("Invalid time. Notification not scheduled.");
            return;
        }

        if (interval < 0) {
            System.out.println("Invalid repeat interval.");
            return;
        }

        String id = "N" + nextId++;
        Notification n = new Notification(id, time, message, interval);

        pq.offer(n);

        if (interval > 0) {
            if (!recurringQueue.enqueue(n)) {
                pq.remove(n);
                System.out.println("Recurring queue is full.");
                return;
            }
        }

        System.out.println("Notification scheduled: " + id + " | " + time);
    }

    static void cancel(String id) {
        boolean found = false;

        Iterator<Notification> it = pq.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (n.id.equalsIgnoreCase(id)) {
                it.remove();
                found = true;
            }
        }

        Iterator<Notification> readyIt = readyQueue.iterator();
        while (readyIt.hasNext()) {
            Notification n = readyIt.next();
            if (n.id.equalsIgnoreCase(id)) {
                readyIt.remove();
                found = true;
            }
        }

        recurringQueue.remove(id);
        cancelled.add(id);

        if (found)
            System.out.println("Notification " + id + " cancelled.");
        else
            System.out.println("Notification not found.");
    }

    static void processDue(String currentTime) {
        if (!validTime(currentTime)) {
            System.out.println("Invalid time.");
            return;
        }

        int currentMinutes = timeToMinutes(currentTime);

        while (!pq.isEmpty() && pq.peek().toMinutes() <= currentMinutes) {
            Notification n = pq.poll();

            if (cancelled.contains(n.id))
                continue;

            readyQueue.offer(n);
        }

        if (readyQueue.isEmpty()) {
            System.out.println("No notifications ready to send.");
            return;
        }

        while (!readyQueue.isEmpty()) {
            Notification n = readyQueue.poll();

            if (cancelled.contains(n.id))
                continue;

            System.out.println("Notification scheduled for "
                    + n.time + " is sent: " + n.message);

            if (n.repeatInterval > 0) {
                Notification reminder = recurringQueue.dequeue();

                if (reminder != null && !cancelled.contains(reminder.id)) {
                    int nextTime = n.toMinutes() + n.repeatInterval;
                    String newTime = minutesToTime(nextTime);

                    reminder.time = newTime;
                    pq.offer(reminder);
                    recurringQueue.enqueue(reminder);
                }
            }
        }
    }

    static int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60
                + Integer.parseInt(parts[1]);
    }

    static String minutesToTime(int minutes) {
        minutes = minutes % (24 * 60);
        int hour = minutes / 60;
        int minute = minutes % 60;

        return String.format("%02d:%02d", hour, minute);
    }

    static void viewPending() {
        if (pq.isEmpty()) {
            System.out.println("No pending notifications.");
            return;
        }

        ArrayList<Notification> list = new ArrayList<>(pq);
        list.sort((a, b) -> {
            int cmp = Integer.compare(a.toMinutes(), b.toMinutes());
            return cmp != 0 ? cmp : a.id.compareTo(b.id);
        });

        System.out.println("\nPending Notifications:");
        for (Notification n : list) {
            System.out.println(n.id + " | " + n.time + " | " + n.message
                    + (n.repeatInterval > 0
                    ? " | Every " + n.repeatInterval + " minutes"
                    : ""));
        }
    }

    static void loadFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists())
            return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\|", 4);

                if (data.length != 4)
                    continue;

                String id = data[0];
                String time = data[1];
                String message = data[2];

                int interval = Integer.parseInt(data[3]);

                if (!validTime(time) || interval < 0)
                    continue;

                Notification n = new Notification(id, time, message, interval);
                pq.offer(n);

                if (interval > 0)
                    recurringQueue.enqueue(n);

                if (id.startsWith("N")) {
                    try {
                        nextId = Math.max(nextId,
                                Integer.parseInt(id.substring(1)) + 1);
                    } catch (NumberFormatException e) {
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error loading notifications.");
        }
    }

    static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Notification n : pq) {
                if (!cancelled.contains(n.id)) {
                    pw.println(n.id + "|" + n.time + "|"
                            + n.message + "|" + n.repeatInterval);
                }
            }

            for (Notification n : readyQueue) {
                if (!cancelled.contains(n.id)) {
                    pw.println(n.id + "|" + n.time + "|"
                            + n.message + "|" + n.repeatInterval);
                }
            }

        } catch (IOException e) {
            System.out.println("Error saving notifications.");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        loadFromFile();

        int choice;

        do {
            System.out.println("\n===== Notification Scheduler =====");
            System.out.println("1. Schedule notification");
            System.out.println("2. Cancel notification");
            System.out.println("3. Process due notifications");
            System.out.println("4. View pending notifications");
            System.out.println("5. View recurring reminders");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            while (!sc.hasNextInt()) {
                sc.next();
                System.out.print("Enter a valid choice: ");
            }

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter time (HH:mm): ");
                    String time = sc.nextLine();

                    System.out.print("Enter message: ");
                    String message = sc.nextLine();

                    System.out.print("Repeat interval in minutes (0 for no repeat): ");
                    int interval;

                    while (!sc.hasNextInt()) {
                        sc.next();
                        System.out.print("Enter a valid interval: ");
                    }

                    interval = sc.nextInt();
                    sc.nextLine();

                    schedule(time, message, interval);
                    break;

                case 2:
                    System.out.print("Enter notification ID: ");
                    cancel(sc.nextLine());
                    break;

                case 3:
                    System.out.print("Enter current time (HH:mm): ");
                    processDue(sc.nextLine());
                    break;

                case 4:
                    viewPending();
                    break;

                case 5:
                    System.out.println("Recurring Reminders:");
                    recurringQueue.display();
                    break;

                case 6:
                    saveToFile();
                    System.out.println("Notifications saved. Exiting...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 6);

        sc.close();
    }
}