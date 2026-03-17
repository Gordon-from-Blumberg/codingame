import java.util.*;

import static java.lang.Math.*;

class Player {
    private static final float RAD_TO_DEG = (float) (180 / PI);

    private static final float POD_RADIUS = 400;
    private static final float CHECKPOINT_RADIUS = 600;
    private static final float FRICTION_COEF = 0.85f;
    private static final float MAX_THRUST = 200;
    private static final float MAX_VELOCITY = FRICTION_COEF * MAX_THRUST / (1 - FRICTION_COEF);
    private static final float MAX_ROTATION_PER_TURN = 18;
    private static final float BREAKING_COEF = 0.77f;

    private static final float MIN_TURN_RADIUS = 1000;
    private static final float TARGET_RADIUS_COEF = 0.95f;

    private static final float BOOST = 650;
    private static final float BOOST_ANGLE = MAX_ROTATION_PER_TURN;

    private static final float ANGLE_LIMIT = 85f;
    private static final float ANGLE_THRUST_COEF = 1f / (ANGLE_LIMIT * ANGLE_LIMIT * ANGLE_LIMIT);
    private static final float REVERSE_VELOCITY_ANGLE = 145;
    private static final float MAX_CORRECTING_ANGLE = 15;
    private static final float CORRECTING_ANGLE_COEF = 0.7f;

    private static final float COLLISION_PREDICTION_FACTOR = 1.3f;
    private static final float COLLISION_DISTANCE = 3.5f * (COLLISION_PREDICTION_FACTOR * MAX_VELOCITY + POD_RADIUS);
    private static final float MIN_VELOCITY_PROJ_FOR_SHIELD = 200;
    private static final float ANGLE_VELOCITY_POD_ANGLE_SIZE_RATIO = 0.3f;
    private static final float MIN_ANGLE_FOR_SHIELD = 50f;
    private static final float EVASION_ANGLE_COEF = 1.0f;

    private static final float MIN_CURRENT_TO_NEXT_THRUST_COEF = 0.25f;

    private static Vec[] checkPoints;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        final int lapsCount = in.nextInt();
        checkPoints = new Vec[in.nextInt()];
        for (int i = 0; i < checkPoints.length; i++) {
            checkPoints[i] = new Vec(in.nextFloat(), in.nextFloat());
        }

        final Pod pod1 = Pod.pods[0] = new Pod(true);
        final Pod pod2 = Pod.pods[1] = new Pod(true, false);

        final Pod opp1 = Pod.pods[2] = new Pod(false);
        final Pod opp2 = Pod.pods[3] = new Pod(false);

        // game loop
        while (true) {
            for (Pod pod : Pod.pods) pod.readData(in);

            debug("pos = %s, vel = %s, angle = %s", pod1.pos, pod1.velocity, pod1.angle);
            pod1.moveToNextCheckpoint();

            debug("pos = %s, vel = %s, angle = %s", pod2.pos, pod2.velocity, pod2.angle);
            pod2.crushToOpp();
//            pod2.output();
            debug("passed = " + pod1.passed + ", " + pod2.passed);
        }
    }

    private static void debug(String str, Object... args) {
        System.err.println(String.format(str, args));
    }

    static class Pod {
        final static Pod[] pods = new Pod[4];
        final static Vec temp = new Vec();
        final static Vec temp2 = new Vec();

        final static Vec directionToPod = new Vec();
        final static Vec relatedVelocityToPod = new Vec();

        final boolean own;
        final boolean evade;

        final Vec pos = new Vec();
        final Vec velocity = new Vec();

        final Vec currentTarget = new Vec();
        final Vec currentTargetDir = new Vec();
        final Vec nextTarget = new Vec();
        final Vec nextTargetDir = new Vec();

        final Vec target = new Vec();
        final Vec targetDir = new Vec();

        float angle;
        int passed = 0;
        int nextCheckPointId;
        Vec nextCheckPoint = new Vec();

        float maxThrust = MAX_THRUST;

        final Output output = new Output();
        final CollisionProcessResult collProcRes = new CollisionProcessResult();

        Pod(boolean own, boolean evade) {
            this.own = own;
            this.evade = evade;
        }

        Pod(boolean own) {this(own, true);}

        void moveToNextCheckpoint() {
            iterate(nextCheckPoint, getNextNextCheckPoint(), CHECKPOINT_RADIUS);
        }

        void crushToOpp() {
            Pod opp = getLeadingOpp();
            Vec target = temp.set(opp.nextCheckPoint);
            Vec nextOppPos = temp2.set(opp.velocity).scale(1.5f).add(opp.pos);

            if (pos.dist2(opp.nextCheckPoint) > nextOppPos.dist2(opp.nextCheckPoint)) {
                Vec oppTarget = opp.nextCheckPoint;
                int i = 0;
                float sumDist = nextOppPos.dist(oppTarget) - CHECKPOINT_RADIUS;
                while (pos.dist(oppTarget) - CHECKPOINT_RADIUS > sumDist) {
                    oppTarget = checkPoints[(opp.nextCheckPointId + ++i) % checkPoints.length];
                    sumDist += nextOppPos.dist(oppTarget) - CHECKPOINT_RADIUS;
                    if (i == checkPoints.length) {
                        oppTarget = opp.getNextNextCheckPoint();
                        break;
                    }
                }
                target.set(oppTarget);
            }

            float t = (float) cos( abs (Vec.getAngle(
                    target.x - nextOppPos.x,
                    target.y - nextOppPos.y,
                    pos.x - nextOppPos.x,
                    pos.y - nextOppPos.y) / RAD_TO_DEG));
            if (t < 0) t = 0;

            debug("opp.pos = %s, opp.nextCP = %s, target pos = %s", opp.pos, opp.nextCheckPoint, target.lerp(opp.pos, t));

            iterate(target, nextOppPos, POD_RADIUS);
        }

        private void iterate(Vec target1, Vec target2, float targetRadius) {
            calcTarget(target1, target2, targetRadius);
            calcThrust();
            handleCollisions();
            output();
        }

        void readData(Scanner in) {
            setPos(in.nextFloat(), in.nextFloat());
            setVelocity(in.nextFloat(), in.nextFloat());
            angle = in.nextFloat();

            int prevCheckPointId = nextCheckPointId;
            nextCheckPointId = in.nextInt();
            if (nextCheckPointId != prevCheckPointId) passed++;
            nextCheckPoint = checkPoints[nextCheckPointId];
        }

        void calcTarget(Vec target1, Vec target2, float targetRadius) {
            targetRadius *= TARGET_RADIUS_COEF;
            currentTarget.set(target1);
            currentTargetDir.set(target1).sub(pos);
            nextTarget.set(target2);
            nextTargetDir.set(target2).sub(target1);

            float currentTargetDistance = currentTargetDir.length();
            float targetAngleSize = (float) asin(targetRadius / currentTargetDistance) * RAD_TO_DEG;
            float velocityTargetAngle = velocity.angle(currentTargetDir);

            targetDir.set(currentTargetDir);
            Vec.add(targetDir, pos, target);

            float rotateAngle;
            if ( abs(velocityTargetAngle) <= targetAngleSize && isBreakingPointInTarget(currentTargetDir, velocity, targetRadius, velocityTargetAngle) ) { // velocity vector towards to target
                rotateAngle = targetDir.angle(nextTargetDir);
                maxThrust = 0.9f * findMaxThrust(targetDir, rotateAngle, targetRadius);
                debug("maxThrust = %s", maxThrust);
            } else {
                rotateAngle = abs(velocityTargetAngle) > REVERSE_VELOCITY_ANGLE ? 180 - velocityTargetAngle : velocityTargetAngle * CORRECTING_ANGLE_COEF;
                if (rotateAngle > MAX_CORRECTING_ANGLE) rotateAngle = MAX_CORRECTING_ANGLE;
                if (rotateAngle < -MAX_CORRECTING_ANGLE) rotateAngle = -MAX_CORRECTING_ANGLE;
                maxThrust = MAX_THRUST;
            }

            debug("rotate for " + rotateAngle);
            rotate(rotateAngle);

            output.target.set(target);
        }

        void calcThrust() {
            float podDirAngleAbs = abs(getPodToDirAngle(targetDir));
            float podDirCoef = getAngleThrustCoef( podDirAngleAbs );
            debug("podDirCoef = " + podDirCoef);

            float thrust = podDirCoef * MAX_THRUST;

            output.setThrust(min(thrust, maxThrust));

            if ( podDirAngleAbs < BOOST_ANGLE
                    && output.target.dist(pos) > getBreakingDist(velocity.length() + BOOST) ) {
                output.boost();
            }
        }

        void handleCollisions() {
            boolean shield = false;
            float finalDirAngle = 360;
            for (Pod pod : pods) {
                if (pod != this) {
                    processCollision(pod);

                    if (pod.collProcRes.shield && !pod.own) shield = true;
                    if (evade && pod.collProcRes.evade) {
                        float dirAngle = directionToPod.angle();
                        debug("dirAngle = " + dirAngle);
                        float evasionAngleByPodVelocity = (float) asin(
                                relatedVelocityToPod.tangProject(directionToPod.x, directionToPod.y) / directionToPod.length()
                        ) * RAD_TO_DEG;

                        if (abs(evasionAngleByPodVelocity / pod.collProcRes.podAngleSize) > ANGLE_VELOCITY_POD_ANGLE_SIZE_RATIO) {
                            debug("evasionAngleByPodVelocity = " + evasionAngleByPodVelocity);
                            finalDirAngle = dirAngle - EVASION_ANGLE_COEF * evasionAngleByPodVelocity;
                        } else {
                            debug("evasionAngle = " + (getAngleDiff(angle, dirAngle)));
                            finalDirAngle = dirAngle - EVASION_ANGLE_COEF * getAngleDiff(angle, dirAngle);
                        }
                    }
                }
            }

            if (shield) output.shield();
            if (finalDirAngle != 360) {
                debug("finalDirAngle = " + finalDirAngle);
                output.target.sub(pos).setAngle(finalDirAngle).add(pos);
            }
        }

        void processCollision(Pod pod) {
            pod.collProcRes.evade = pod.collProcRes.shield = false;
            float podDistance = pos.dist(pod.pos);
            if (isCollisionSoon(pod, podDistance)) {
                float targToVelAngle = abs( currentTargetDir.angle(velocity) );
                float dirToPodToVelAngle = abs( directionToPod.angle(velocity) );
                debug("targToVelAngle = %s, dirToPodToVelAngle = %s", targToVelAngle, dirToPodToVelAngle);
                // pod.collProcRes.evade = targToVelAngle <= MIN_ANGLE_FOR_SHIELD
                //         && dirToPodToVelAngle <= MIN_ANGLE_FOR_SHIELD;
                pod.collProcRes.shield = isNeedShield(podDistance);
            }
        }

        boolean isCollisionSoon(Pod pod, float podDistance) {
            return isPodTooClose(podDistance) && doesMoveTowards(pod, podDistance);
        }

        boolean isPodTooClose(float podDistance) {
            return podDistance <= COLLISION_DISTANCE;
        }

        boolean doesMoveTowards(Pod pod, float podDistance) {
            Vec direction = directionToPod.set(pod.pos).sub(pos);
            Vec relatedVelocity = relatedVelocityToPod.set(velocity).sub(pod.velocity);
            if (Vec.dot(direction.x, direction.y, relatedVelocity.x, relatedVelocity.y) <= 0)
                return false;

            pod.collProcRes.podAngleSize = (float) asin(2 * POD_RADIUS / podDistance) * RAD_TO_DEG;
            return abs(relatedVelocity.angle(direction)) <= pod.collProcRes.podAngleSize;
        }

        boolean isNeedShield(float podDistance) {
            float velocityProj = relatedVelocityToPod.project(directionToPod.x, directionToPod.y);
            return velocityProj > MIN_VELOCITY_PROJ_FOR_SHIELD
                    && (podDistance - 2 * POD_RADIUS) / velocityProj < COLLISION_PREDICTION_FACTOR;
        }

        float getPodToDirAngle(Vec dir) {
            return getAngleDiff(angle,  dir.angle());
        }

        float getAngleDiff(float angle1, float angle2) {
            return normalizeAngle(angle2 - angle1);
        }

        float normalizeAngle(float angle) { //return -180 <= angle <= 180
            angle %= 360;
            if (angle < -180) angle += 360;
            if (angle > 180) angle -= 360;
            return angle;
        }

        float getAngleThrustCoef(float angle) {
            angle = abs(angle);
            return angle > ANGLE_LIMIT
                    ? 0
                    : 1 - ANGLE_THRUST_COEF * angle * angle * angle;
        }

        void rotate(float angle) {
            targetDir.rotate(angle);
            target.set(pos).add(targetDir);
        }

        void setPos(float x, float y) {
            pos.x = x;
            pos.y = y;
        }

        void setVelocity(float x, float y) {
            velocity.x = x;
            velocity.y = y;
        }

        boolean isBreakingPointInTarget(Vec targetDir, Vec velocity, float targetRadius, float velocityTargetAngle) {
            float targetDist = targetDir.length();
            float targetDistCos = targetDist * (float) cos(velocityTargetAngle / RAD_TO_DEG);
            float D = targetRadius * targetRadius - targetDist * targetDist + targetDistCos * targetDistCos;
            if (D < 0) return false;

            float root = (float) sqrt(D);
            float distance = targetDistCos - root;
            float breakingDist = getBreakingDist(velocity.length());
            debug("distance = %s, breakingDist = %s", distance, breakingDist);
            return BREAKING_COEF * breakingDist >= distance;
        }

        boolean isBreakingPointInTarget(Vec targetDir, Vec velocity, float targetRadius) {
            return isBreakingPointInTarget(targetDir, velocity, targetRadius, velocity.angle(targetDir));
        }

        int getBreakingDist(float velocity) {
            int v = (int) velocity;
            int dist = 0;
            while (v > 0) {
                dist += v;
                v *= FRICTION_COEF;
            }
            return dist;
        }

        float findMaxThrust(Vec targetDir, float rotateAngle, float targetRadius) {
            if (rotateAngle > MAX_ROTATION_PER_TURN) rotateAngle = MAX_ROTATION_PER_TURN;
            if (rotateAngle < -MAX_ROTATION_PER_TURN) rotateAngle = -MAX_ROTATION_PER_TURN;

            Vec thrustDir = temp.set(1, 0).setAngle(angle + rotateAngle);
            float thrust = 100, step = 50;

            while( step > 1 ) {
                if (isBreakingPointInTarget(targetDir, temp2.set(thrustDir).scale(thrust).add(velocity), targetRadius)) {
                    thrust += step;
                } else {
                    thrust -= step;
                }

                if (thrust >= 100) {
                    thrust = 100;
                    break;
                }

                step /= 2;
            }

            return thrust;
        }

        Vec getNextNextCheckPoint() {
            return checkPoints[(nextCheckPointId + 1) % checkPoints.length];
        }

        Pod getLeadingOpp() {
            if (pods[2].passed > pods[3].passed) return pods[2];
            if (pods[2].passed < pods[3].passed) return pods[3];
            return pods[2].pos.dist2(pods[2].nextCheckPoint) < pods[3].pos.dist2(pods[3].nextCheckPoint)
                    ? pods[2]
                    : pods[3];
        }

        void output() {
            output.print();
        }
    }

    static class Vec {
        static final Vec temp = new Vec();
        float x, y;
        Vec() {x = y = 0;}
        Vec(float x, float y) {
            this.x = x;
            this.y = y;
        }

        static Vec sub(Vec v1, Vec v2, Vec res) {
            res.x = v1.x - v2.x;
            res.y = v1.y - v2.y;
            return res;
        }

        static Vec add(Vec v1, Vec v2, Vec res) {
            res.x = v1.x + v2.x;
            res.y = v1.y + v2.y;
            return res;
        }

        static float dot(float x1, float y1, float x2, float y2) {
            return x1 * x2 + y1 * y2;
        }

        static float dist2(float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1;
            return dx * dx + dy * dy;
        }

        static float cross(float x1, float y1, float x2, float y2) {
            return x1 * y2 - x2 * y1;
        }

        static float getAngleRad(float x1, float y1, float x2, float y2) {
            float dotProduct = dot(x1, y1, x2, y2);
            float length1 = dist2(0, 0, x1, y1);
            float length2 = dist2(0, 0, x2, y2);
            if (length1 == 0 || length2 == 0) return 0;
            float angleCos = dotProduct / (float) sqrt( length1 * length2 );
            float angle = (float) acos(angleCos);
            if (cross(x1, y1, x2, y2) < 0) {
                angle *= -1;
            }
            return angle;
        }

        static float getAngle(float x1, float y1, float x2, float y2) {
            return getAngleRad(x1, y1, x2, y2) * RAD_TO_DEG;
        }

        Vec set(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        Vec set(Vec v) {
            return set(v.x, v.y);
        }

        Vec add(Vec v) {
            return add(this, v, this);
        }

        Vec add(float x, float y) {
            this.x += x;
            this.y += y;
            return this;
        }

        Vec sub(Vec v) {
            return sub(this, v, this);
        }

        Vec scale(float k) {
            x *= k;
            y *= k;
            return this;
        }

        Vec normalize() {
            float len = length();
            x /= len;
            y /= len;
            return this;
        }

        float project(float x, float y) {
            temp.set(x, y).normalize();
            return dot(this.x, this.y, temp.x, temp.y);
        }

        float tangProject(float x, float y) {
            temp.set(x, y).normalize();
            return cross(temp.x, temp.y, this.x, this.y);
        }

        Vec setLength(float length) {
            return scale( length / length() );
        }

        float length2() {
            return x * x + y * y;
        }

        float length() {
            return (float) sqrt( length2() );
        }

        float dist2(Vec v) {
            float dx = v.x - x, dy = v.y - y;
            return dx * dx + dy * dy;
        }

        float dist(Vec v) {
            return (float) sqrt( dist2(v) );
        }

        Vec lerp(Vec v, float t) {
            x += (v.x - x) * t;
            y += (v.y - y) * t;
            return this;
        }

        Vec setAngle(float angle) {
            float angleRad = angle / RAD_TO_DEG;
            float length = length();
            x = length * (float) cos(angleRad);
            y = length * (float) sin(angleRad);
            return this;
        }

        float angle() {
            return getAngle(1f, 0f, x, y);
        }

        float angle(Vec v) {
            return getAngle(x, y, v.x, v.y);
        }

        Vec rotate(float angle) {
            float radians = angle / RAD_TO_DEG;
            float cos = (float) cos(radians);
            float sin = (float) sin(radians);

            temp.x = x * cos - y * sin;
            temp.y = x * sin + y * cos;

            return this.set(temp);
        }

        public String toString() {
            return x + ", " + y;
        }
    }

    private static class Output {
        private static final String BOOST = "BOOST";
        private static final String SHIELD = "SHIELD";

        private final Vec target = new Vec();
        private float thrust;
        private String thrustStr = "0";

        void setThrust(float thrust) {
            this.thrust = thrust;
            this.thrustStr = String.valueOf( round(thrust) );
        }

        void boost() {
            thrustStr = BOOST;
        }

        void shield() {
            thrustStr = SHIELD;
        }

        void print() {
            System.out.println(round(target.x) + " " + round(target.y) + " " + thrustStr);
        }
    }

    private static class CollisionProcessResult {
        boolean evade;
        boolean shield;
        float podAngleSize;
    }
}