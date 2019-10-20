import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class ShapeWrapper {
    final Color color;
    Shape shape;
    List<Animation> animations;

    public ShapeWrapper(Color color, Shape shape) {
        this.color = color;
        this.shape = shape;
    }

    double getX() {
        if (shape instanceof Rectangle) {
            return ((Rectangle) shape).getX() + ((Rectangle) shape).getWidth() / 2;
        } else if (shape instanceof Ellipse2D) {
            return ((Ellipse2D) shape).getX() + ((Ellipse2D) shape).getWidth() / 2;
        } else {
            throw new IllegalStateException("Only rectangles and circles are allowed");
        }
    }

    double getY() {
        if (shape instanceof Rectangle) {
            return ((Rectangle) shape).getY() + ((Rectangle) shape).getHeight() / 2;
        } else if (shape instanceof Ellipse2D) {
            return ((Ellipse2D) shape).getY() + ((Ellipse2D) shape).getHeight() / 2;
        } else {
            throw new IllegalStateException("Only rectangles and circles are allowed");
        }
    }

    void move(double x, double y) {
        if (shape instanceof Rectangle) {
            ((Rectangle) shape).setLocation((int) (x - ((Rectangle) shape).getWidth() / 2), (int) (y - ((Rectangle) shape).getHeight() / 2));
        } else if (shape instanceof Ellipse2D) {
            shape = new Ellipse2D.Double(
                    x - ((Ellipse2D) shape).getWidth() / 2,
                    y - ((Ellipse2D) shape).getHeight() / 2,
                    ((Ellipse2D) shape).getWidth(),
                    ((Ellipse2D) shape).getHeight());
        } else {
            throw new IllegalStateException("Only rectangles and circles are allowed");
        }
    }
}

abstract class Animation {
    protected ShapeWrapper wrapper;
    protected long time;
    protected final boolean cycle;

    public Animation(ShapeWrapper wrapper, long time, boolean cycle) {
        this.wrapper = wrapper;
        this.time = time;
        this.cycle = cycle;
    }

    abstract AffineTransform draw(Graphics2D g, Long curTime);

}

class MoveAnimation extends Animation {
    double sourceX;
    double sourceY;

    double destX;
    double destY;

    double length;
    double speed;
    private long start = 0L;

    public MoveAnimation(ShapeWrapper shape, long time, boolean cycle) {
        super(shape, time, cycle);
    }

    void setSource(double x, double y) {
        sourceX = x;
        sourceY = y;
        init();
    }

    void setDest(double x, double y) {
        destX = x;
        destY = y;
        init();
    }

    void init() {
        length = Math.sqrt((destX - sourceX) * (destX - sourceX) + (destY - sourceY) * (destY - sourceY));
        speed = length / time;
    }

    @Override
    AffineTransform draw(Graphics2D g, Long curTime) {
        if (start == 0L) {
            start = curTime;
            wrapper.move(sourceX, sourceY);
        }

        if (curTime - start < time) {
            double normalizedDirectionX = destX - sourceX;
            double normalizedDirectionY = destY - sourceY;
            double normilizedDirectionLen = Math.sqrt(normalizedDirectionX * normalizedDirectionX + normalizedDirectionY * normalizedDirectionY);
            normalizedDirectionX /= normilizedDirectionLen;
            normalizedDirectionY /= normilizedDirectionLen;

            wrapper.move(
                    sourceX + normalizedDirectionX * speed * (curTime - start),
                    sourceY + normalizedDirectionY * speed * (curTime - start));
        } else {
            wrapper.move(destX, destY);
            if (cycle) {
                double temp = destY;
                destY = sourceY;
                sourceY = temp;

                temp = destX;
                destX = sourceX;
                sourceX = temp;

                start += time;
            }
        }
        return new AffineTransform();
    }
}

class RotateAnimation extends Animation {
    private double destAngle;
    private double srcAngle = 0L;
    private Long start = 0L;

    public RotateAnimation(ShapeWrapper wrapper, long time, boolean cycle) {
        super(wrapper, time, cycle);
    }

    void setDestAngle(double destAngle) {
        this.destAngle = destAngle * Math.PI / 180.0;
    }

    public void setSrcAngle(double srcAngle) {
        this.srcAngle = srcAngle * Math.PI / 180.0;
    }

    @Override
    AffineTransform draw(Graphics2D g, Long curTime) {
        if (start == 0L) {
            start = curTime;
        }
        AffineTransform tr = g.getTransform();
        if (curTime - start < time) {
            double speed = (destAngle - srcAngle) / time;
            tr.rotate(srcAngle + speed * (curTime - start), wrapper.getX(), wrapper.getY());
        } else {
            tr.rotate(destAngle, wrapper.getX(), wrapper.getY());
            if (cycle) {
                double tmp = this.srcAngle;
                srcAngle = destAngle;
                destAngle = tmp;
                start += time;
            }
        }
        return tr;
    }
}

class ScaleAnimation extends Animation {
    double srcScale;
    private double destScale;
    private Long start = 0L;

    public ScaleAnimation(ShapeWrapper wrapper, long time, boolean cycle) {
        super(wrapper, time, cycle);
    }

    public void setDestScale(double destScale) {
        this.destScale = destScale;
    }

    @Override
    AffineTransform draw(Graphics2D g, Long curTime) {
        if (start == 0L) {
            start = curTime;
        }

        AffineTransform tr = g.getTransform();
        if (curTime - start < time) {
            double speed = (destScale - srcScale) / time;
            double scaleFactor = srcScale + speed * (curTime - start);
            tr.translate(wrapper.getX(), wrapper.getY());
            tr.scale(scaleFactor, scaleFactor);
            tr.translate(-wrapper.getX(), -wrapper.getY());
            g.setTransform(tr);
        } else {
            tr.translate(wrapper.getX(), wrapper.getY());
            tr.scale(destScale, destScale);
            tr.translate(-wrapper.getX(), -wrapper.getY());
            g.setTransform(tr);
            if (cycle) {
                double tmp = this.destScale;
                destScale = srcScale;
                srcScale = tmp;
                start += time;
            }
        }
        return tr;
    }
}

public class Foo extends JPanel implements ActionListener {
    private static final int RECT_X = 20;
    private static final int RECT_Y = RECT_X;
    private static int RECT_WIDTH = 1000;
    private static int RECT_HEIGHT = RECT_WIDTH;
    private static final ArrayList<ShapeWrapper> figures = new ArrayList<>();

    Timer time = new Timer(1, this);

    public Foo() {
        time.start();
    }

    static Color textToColor(String name) {
        switch (name) {
            case "yellow":
                return Color.YELLOW;
            case "black":
                return Color.BLACK;
            case "red":
                return Color.RED;
            case "white":
                return Color.WHITE;
            default:
                throw new IllegalArgumentException("Invalid color " + name);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        long curTime = System.currentTimeMillis();
        for (ShapeWrapper figure : figures) {
            AffineTransform backup = (AffineTransform) g2d.getTransform().clone();
            AffineTransform tr = new AffineTransform();
            for (Animation animation : figure.animations) {
                AffineTransform draw = animation.draw(g2d, curTime);
                tr.concatenate(draw);
            }
            g2d.setTransform(tr);
            g2d.setColor(figure.color);
            g2d.fill(figure.shape);
            g2d.setTransform(backup);
        }
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // so that our GUI is big enough
        return new Dimension(RECT_WIDTH + 2 * RECT_X, RECT_HEIGHT + 2 * RECT_Y);
    }

    // create the GUI explicitly on the Swing event thread
    private static void createAndShowGui() {
        Foo mainPanel = new Foo();

        JFrame frame = new JFrame("DrawRect");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        List<Integer> tempList = Arrays.stream(line.split(" "))
                .map(it -> Integer.parseInt(it.trim()))
                .collect(Collectors.toList());
        RECT_WIDTH = tempList.get(0);
        RECT_HEIGHT = tempList.get(1);
        int figureCount = Integer.parseInt(scanner.nextLine().trim());
        for (int i = 0; i < figureCount; ++i) {
            String[] input = scanner.nextLine().split(" ");
            ShapeWrapper last;
            if (input[0].equals("rectangle")) {
                // rectangle centerX centerY width height angle color
                int centerX = (int) Double.parseDouble(input[1]);
                int centerY = (int) Double.parseDouble(input[2]);
                int width = (int) Double.parseDouble(input[3]);
                int height = (int) Double.parseDouble(input[4]);

                Rectangle rect = new Rectangle(centerX - width / 2, centerY - height / 2, width, height);

                last = new ShapeWrapper(textToColor(input[6]), rect);
                figures.add(last);
            } else {
                // circle centerX centerY radius color

                double centerX = Double.parseDouble(input[1]);
                double centerY = Double.parseDouble(input[2]);
                double radius = Double.parseDouble(input[3]);
                Color color = textToColor(input[4]);
                Ellipse2D.Double ellipse = new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
                last = new ShapeWrapper(color, ellipse);
                figures.add(last);
            }
            List<Animation> animations = new ArrayList<>();
            int animationsCount = Integer.parseInt(scanner.nextLine().trim());
            for (int j = 0; j < animationsCount; ++j) {
                String[] s = scanner.nextLine().split(" ");
                if (s[0].equals("move")) {
                    // move destX destY time [cycle]
                    MoveAnimation e = new MoveAnimation(last, (long) Double.parseDouble(s[3]), s.length >= 5);
                    e.setSource(last.getX(), last.getY());
                    e.setDest(Double.parseDouble(s[1]), Double.parseDouble(s[2]));
                    animations.add(e);
                } else if (s[0].equals("rotate")) {
                    // rotate angle time [cycle]
                    RotateAnimation e = new RotateAnimation(last, (long) Double.parseDouble(s[2]), s.length >= 4);
                    double angle = Double.parseDouble(s[1]);
                    e.setDestAngle(0);
                    e.setSrcAngle(-angle);
                    animations.add(e);
                } else { // scale
                    // scale destScale time [cycle]
                    ScaleAnimation e = new ScaleAnimation(last, (long) Double.parseDouble(s[2]), s.length >= 4);
                    e.setDestScale(Double.parseDouble(s[1]));
                    e.srcScale = 1.0;
                    animations.add(e);
                }
            }
            animations.sort(new Comparator<Animation>() {
                private int priority(Animation animation) {
                    if (animation instanceof MoveAnimation) {
                        return 0;
                    }
                    if (animation instanceof RotateAnimation) {
                        return 1;
                    }
                    if (animation instanceof ScaleAnimation) {
                        return 2;
                    }
                    throw new IllegalStateException("Unknown animation: " + animation);
                }

                @Override
                public int compare(Animation x, Animation y) {
                    return Integer.compare(priority(x), priority(y));
                }
            });
            last.animations = animations;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGui();
            }
        });
    }
}
