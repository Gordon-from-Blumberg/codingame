using System;
using System.Linq;
using System.IO;
using System.Text;
using System.Collections;
using System.Collections.Generic;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player
{
    static float GRAVITY = 3.711f;
    static int MAX_V_LANDING_SPEED = 38;
    static int MAX_H_LANDING_SPEED = 19;
    static int MAX_ROTATE = 90;
    static int MAX_ROTATE_PER_TURN = 15;
    static int MAX_ROTATE_WITH_GRAVITY_COMPENSATION = 21;
    static int LANDING_AREA = 50;
    static float MAX_H_ACCELERATION = 1.5f;
    static float MAX_V_ACCELERATION = 4 - GRAVITY;
    static int SAFE_V_DIST = 50;

    static int CORRECTING_TIME = 6;
    
    static void Main(string[] args)
    {
        int surfaceN = int.Parse(Console.ReadLine()); // the number of points used to draw the surface of Mars.
        Point[] points = new Point[surfaceN];
        FlatSurface flatSurface = null;
        for (int i = 0; i < surfaceN; i++)
        {
            string[] inputs = Console.ReadLine().Split(' ');
            points[i] = new Point(int.Parse(inputs[0]), int.Parse(inputs[1]));
            
            if (i > 0 && points[i].y == points[i - 1].y
                    && points[i].x - points[i - 1].x >= 1000)
            {
                flatSurface = new FlatSurface(points[i - 1], points[i]);
            }
        }

        int rotate = 0, power = 0;
        int maxRotate = MAX_ROTATE;
        Point targetPoint = new Point();
        Point currentPoint = new Point();

        while (true)
        {
            string input = Console.ReadLine();
            Debug("input = " + input);
            string[] inputs = input.Split(' ');
            currentPoint.x = int.Parse(inputs[0]);
            currentPoint.y = int.Parse(inputs[1]);
            int hSpeed = int.Parse(inputs[2]); // the horizontal speed (in m/s), can be negative.
            int vSpeed = int.Parse(inputs[3]); // the vertical speed (in m/s), can be negative.
            int fuel = int.Parse(inputs[4]); // the quantity of remaining fuel in liters.
            int angle = int.Parse(inputs[5]); // the rotation angle in degrees (-90 to 90).
            int currentPower = int.Parse(inputs[6]); // the thrust power (0 to 4).
            
            float speedRatio = vSpeed == 0 ? Math.Abs(hSpeed) : Math.Abs(hSpeed / vSpeed);
            int targetHSpeed, targetVSpeed;
            if (!flatSurface.IsAbove(currentPoint.x))
            {
                SetTargetPoint(currentPoint, points, flatSurface, targetPoint);
                
                int targetMovementX = targetPoint.x - currentPoint.x;
                int targetMovementY = targetPoint.y - currentPoint.y;

                float distRatio = Math.Abs(targetMovementX / targetMovementY);
                Debug("distRatio = " + distRatio);

                int maxHVel = GetMaxVelocity(MAX_H_LANDING_SPEED, MAX_H_ACCELERATION, Math.Abs(flatSurface.GetDistX(currentPoint.x)));
                int maxVVel = GetMaxVelocity(MAX_V_LANDING_SPEED, MAX_V_ACCELERATION, Math.Abs(targetMovementY));
                maxVVel = Math.Min(maxVVel, MAX_V_LANDING_SPEED);

                Debug("maxHVel = " + maxHVel);
                Debug("maxVVel = " + maxVVel);

                if (speedRatio > distRatio)
                {
                    targetVSpeed = maxVVel * Math.Sign(targetMovementY);
                    targetHSpeed = (int) Math.Round(distRatio * targetVSpeed * Math.Sign(targetMovementX));
                } else 
                {
                    targetHSpeed = maxHVel * Math.Sign(targetMovementX);
                    targetVSpeed = (int) Math.Round(targetHSpeed / distRatio * Math.Sign(targetMovementY));
                }
            } else
            {
                targetVSpeed = -MAX_V_LANDING_SPEED;
                targetHSpeed = 0;
                // if (hSpeed != 0)
                // {                    
                //     int limitX = hSpeed > 0 ? flatSurface.GetRightX() : flatSurface.GetLeftX();
                //     Debug("limitX = " + limitX);
                //     float distRatio = Math.Abs((limitX - currentPoint.x) / (flatSurface.y - currentPoint.y));
                //     targetHSpeed = distRatio < speedRatio 
                //         ? Math.Abs(hSpeed)
                //         : GetMaxVelocity(MAX_H_LANDING_SPEED, MAX_H_ACCELERATION, Math.Abs(limitX - currentPoint.x));
                //     targetHSpeed = Math.Min(targetHSpeed, MAX_H_LANDING_SPEED) * Math.Sign(hSpeed);
                // } else
                // {
                //     targetHSpeed = 0;
                // }
            }

            Debug("targetHSpeed = " + targetHSpeed);
            Debug("targetVSpeed = " + targetVSpeed);

            int vSpeedDiff = targetVSpeed - vSpeed;
            int hSpeedDiff = targetHSpeed - hSpeed;
            float diffRatio = Math.Abs(vSpeedDiff == 0 ? hSpeedDiff : hSpeedDiff / vSpeedDiff);

            if (vSpeedDiff > 0)
            {
                rotate = (int) (diffRatio <= 1 ? 30 * diffRatio : 30 + (diffRatio - 1) * 7);
            } else
            {
                rotate = MAX_ROTATE;
            }

            if (vSpeed < 0) maxRotate = vSpeed * (MAX_ROTATE - MAX_ROTATE_WITH_GRAVITY_COMPENSATION) + MAX_ROTATE;
            if (maxRotate < MAX_ROTATE_WITH_GRAVITY_COMPENSATION) maxRotate = MAX_ROTATE_WITH_GRAVITY_COMPENSATION;

            if (rotate > maxRotate) rotate = maxRotate;
            rotate *= -Math.Sign(hSpeedDiff);

            if (GetTime(currentPoint.y, flatSurface.y, vSpeed) <= CORRECTING_TIME) rotate = 0;

            power = 4;
            int powerCoef = Math.Abs(rotate - angle) / (3 * MAX_ROTATE_PER_TURN) - 1;
            if (powerCoef > 0) power -= powerCoef;
            if (power < 0) power = 0;

            Console.WriteLine(rotate + " " + power);
        }
    }
    
    static void Debug(string msg)
    {
        Console.Error.WriteLine(msg);
    }
    
    static void SetTargetPoint(Point current, Point[] points, FlatSurface flat, Point target)
    {
        int curPointIndex = 0;
        for (int i = 0; i < points.Length - 1; i++)
        {
            if (points[i].x <= current.x && current.x < points[i + 1].x)
            {
                curPointIndex = i;
                break;
            }
        }

        Point curPoint = points[curPointIndex];
                
        Debug("curPoint.x = " + curPoint.x);
    
        int underY = curPoint.y + (current.x - curPoint.x) * (points[curPointIndex + 1].y - curPoint.y) / (points[curPointIndex + 1].x - curPoint.x);
        int pickY = 0, pickX = -1;
        if (current.x < flat.start.x)
        {
            for (int i = curPointIndex + 1; points[i].x < flat.start.x; i++)
            {
                if (points[i].y > pickY) 
                {
                    pickX = points[i].x;
                    pickY = points[i].y;
                }
            }
        } else
        {
            for (int i = curPointIndex; points[i].x > flat.end.x; i--)
            {
                if (points[i].y > pickY) 
                {
                    pickX = points[i].x;
                    pickY = points[i].y;
                }
            }
        }

        Debug("pickX = " + pickX);
        Debug("pickY = " + pickY);

        if (pickY > flat.y)
        {
            target.x = pickX;
            target.y = pickY + SAFE_V_DIST;
        } else
        {
            target.x = flat.GetClosestX(current.x);
            target.y = flat.y + SAFE_V_DIST;
        }
    }

    static int GetMaxVelocity(int targetVel, float maxAcceleration, int distance)
    {
        return (int) Math.Round(Math.Sqrt(targetVel * targetVel + 2 * maxAcceleration * distance));
    }

    static int GetTime(int y, int targetY, int vSpeed) 
    {
        return vSpeed == 0 ? y - targetY : (y - targetY) / Math.Abs(vSpeed);
    }
    
    class Point
    {
        public int x, y;
        public Point() {x = -1; y = -1;}
        public Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    class FlatSurface
    {
        public Point start, end;
        public int y, width;
        public FlatSurface(Point start, Point end)
        {
            this.start = start;
            this.end = end;
            this.y = start.y;
            this.width = end.x - start.x;
        }

        public bool IsAbove(int x) 
        {
            return start.x + LANDING_AREA < x && x < end.x - LANDING_AREA;
        }

        public int GetClosestX(int x)
        {
            return x < start.x + LANDING_AREA 
                    ? start.x + LANDING_AREA 
                    : x > end.x - LANDING_AREA 
                        ? end.x - LANDING_AREA 
                        : x;
        }

        public int GetDistX(int x)
        {
            return x < start.x + LANDING_AREA 
                    ? start.x + LANDING_AREA - x 
                    : x > end.x - LANDING_AREA 
                        ? x - end.x + LANDING_AREA 
                        : 0;
        }

        public int GetRightX()
        {
            return end.x - LANDING_AREA;
        }

        public int GetLeftX()
        {
            return start.x + LANDING_AREA;
        }

        public int GetLandingWidth()
        {
            return width - 2 * LANDING_AREA;
        }
    }
}
