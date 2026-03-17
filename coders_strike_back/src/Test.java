import static java.lang.Math.*;

public class Test {
    static final float RAD_TO_DEG = (float) (180 / PI);
    static final float FRICTION_COEF = 0.85f;

    public static void main(String[] args) {
        Player.Vec pos = new Player.Vec();
        Player.Vec velocity = new Player.Vec(500, 0);
//        Player.Pod pod = new Player.Pod();

        float angle = 0;

        float omega = 0;
        float thrust = 0;

        int iterations = 0;

        test(50);
        test(100);
        test(120);
        test(237);

//        System.out.println(String.format("Vec.getAngle() = %s", Player.Vec.getAngle(1, 0, 1, -1)));

//        while (iterations-- > 0) {
//            float rotation = omega > 18 ? 18 : omega < -18 ? -18 : omega;
//            angle += rotation;
//
//            float angleRad = angle / RAD_TO_DEG;
//            velocity.add(thrust * (float) cos(angleRad), thrust * (float) sin(angleRad));
//
//            pos.add(velocity);
//
//            velocity.scale(0.85f);
//
//            System.out.println(String.format("pos = %s, velocity = %s", pos, velocity));
//            System.out.println(String.format("angle = %s, velocity angle = %s", angle, velocity.angle()));
//            pod.velocity.set(velocity);
//            System.out.println(String.format("breaking dist = %s", pod.getBreakingDist(1, 0)));
//        }

    }

    private static float angleBetween(float angle1, float angle2) {
        float diff = (angle2 - angle1) % 360;
        if (diff < 0) diff += 360;
        if (diff > 180) diff -= 360;
        return diff;
    }

    private static float getBreakingDist(float velocity) {
        return velocity / (1 - FRICTION_COEF);
    }

    private static int getBreakingDistInt(float velocity) {
        int s = 0;
        int vel = (int) velocity;
        while (vel > 0) {
            s += vel;
            vel *= FRICTION_COEF;
        }
        return s;
    }

    private static void test(float velocity) {
        Player.debug("float breaking dist for velocity %s = %s", velocity, getBreakingDist(velocity));
        Player.debug("int breaking dist for velocity %s = %s", velocity, getBreakingDistInt(velocity));
    }
}
