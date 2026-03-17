String.prototype.replaceAt = function(index, replacement) {
    return this.substr(0, index) + replacement + this.substr(index + replacement.length);
}

const height = parseInt(readline());
const width = parseInt(readline());
const n = parseInt(readline());

console.error(`width = ${width}, height = ${height}`);

const scheme = { 
    _ : [],
    get: function(x, y) { return this._[y] && this._[y].charAt(x) },
    set: function(x, y, s) {
        this._[y] = this._[y].replaceAt(x, s);
    }
};

for (let i = 0; i < height; i++) {
    scheme._.push(new Array(width + 1).join('?'));
}

const coords = [];
for (let i = 0; i < n; i++)
    coords.push({});

const myCoords = coords[n - 1];

const dirs = [
    {x: 0, y: -1, c: 'C', dir: 'up'}, 
    {x: 1, y: 0, c: 'A', dir: 'right'},
    {x: 0, y: 1, c: 'D', dir: 'down'},
    {x: -1, y: 0, c: 'E', dir: 'left'}
];

while (true) {
    for (let d of dirs) 
        d.type = readline();

    for (let i = 0; i < n; i++) {
        var inputs = readline().split(' ');
        coords[i].x = parseInt(inputs[0]);
        coords[i].y = parseInt(inputs[1]);

        // console.error(`coords: x = ${coords[i].x}, y = ${coords[i].y}`);

        // scheme.set(coords[i].x, coords[i].y, '_');
    }
    scheme.set(myCoords.x, myCoords.y, '_');
    forEachDir(myCoords.x, myCoords.y, (x, y, d) => scheme.set(x, y, d.type));

    for (let y = 0; y < height; y++) {
        row = scheme._[y].replace(/_/g, ' ');
        for (let c of coords) {
            if (y == c.y) {
                row = row.replaceAt(c.x, c == myCoords ? '*' : '$');
            }
        }
        console.error(row);
    }

    let start = new Date();
    const info = analyze();
    const enemyMarks = [];

    for (let i = 0; i < n - 1; i++) {
        enemyMarks.push(generateMarks(coords[i].x, coords[i].y));
    }

    console.error(`marking took ${new Date() - start} ms`);
    
    start = new Date();
    let res = 'B';
    let pathInfos = [];

    for (let i = 0; i < info.closestUnknown.length; i += 2) {
        const pathInfo = {};
        pathInfo.path = restorePath(info.marks, info.closestUnknown[i], info.closestUnknown[i + 1]);
        pathInfo.prioPathLength = 12 / pathInfo.path.length;

        let firstStepD = height + width;
        pathInfo.prioMinD = 0;

        let minD = height + width;
        let stepNumber = 1;

        for (let enemyMark of enemyMarks) {

            if (info.marks.get(enemyMark.x, enemyMark.y) > 8) continue;

            for (let j = 0; j < pathInfo.path.length; j += 2) {
                if (enemyMark.get(pathInfo.path[j], pathInfo.path[j + 1]) < minD) {
                    minD = enemyMark.get(pathInfo.path[j], pathInfo.path[j + 1]);
                    stepNumber = j / 2;
                }
            }

            if (enemyMark.get(pathInfo.path[0], pathInfo.path[1]) < firstStepD) {
                firstStepD = enemyMark.get(pathInfo.path[0], pathInfo.path[1]);
            }
        }

        pathInfo.prioMinD = 0;//-10 / ((minD + 1) * (stepNumber + 3));

        pathInfo.prioFirstStep = firstStepD <= 1 ? -1000 : -10 / firstStepD;
        pathInfo.priority = pathInfo.prioPathLength + pathInfo.prioMinD + pathInfo.prioFirstStep;
        pathInfos.push(pathInfo);
    }

    pathInfos = pathInfos.sort((pi1, pi2) => pi2.priority - pi1.priority);
    for (let pi of pathInfos) {
        console.error(`x = ${pi.path[pi.path.length - 2]}, y = ${pi.path[pi.path.length - 1]}: length = ${pi.prioPathLength}, minD = ${pi.prioMinD}, first = ${pi.prioFirstStep}, SUM = ${pi.priority}`);
        if (pi.path[pi.path.length - 2] == 17 && pi.path[pi.path.length - 1] == 11) console.error(pi.path);
    }

    // pathInfos = pathInfos.filter(pi => pi.priority > 0);

    if (pathInfos.length > 0) {
        const targetPath = pathInfos[0].path;
        let dx = targetPath[0] - myCoords.x;
        if (dx < -1) dx += width;
        if (dx > 1) dx -= width;
        let dy = targetPath[1] - myCoords.y;
        if (dy < -1) dy += height;
        if (dy > 1) dy -= height;
        
        for (let d of dirs) {
            if (d.x == dx && d.y == dy) {
                res = d.c;
            }
        }

    } else {
        let maxMinD = 0;
        forEachDir(myCoords.x, myCoords.y, (x, y, d) => {
            if (d.type == '_') {
                let minD = height + width;

                for (let enemyMark of enemyMarks) {
                    if (enemyMark.get(x, y) < minD) minD = enemyMark.get(x, y);
                }

                if (minD > maxMinD) {
                    maxMinD = minD;
                    res = d.c;
                }
            }
        });
    }

    console.error(`decision making took ${new Date() - start} ms`);

    console.log(res);
}

function analyze() {
    console.error(`analyze from x = ${myCoords.x}, y = ${myCoords.y}`);
    const result = {
        closestUnknown: []
    };

    result.marks = generateMarks(myCoords.x, myCoords.y, null, null, (cx, cy, x, y) => {
        if (scheme.get(cx, cy) == '_' && scheme.get(x, y) == '?') {
            result.closestUnknown.push(x, y);
        }
    });

    return result;
}

function findPath(fromX, fromY, toX, toY) {
    return restorePath(generateMarks(fromX, fromY, toX, toY), toX, toY);
}

function restorePath(marks, x, y) {
    const path = [x, y];
    let mark = marks.get(x, y);

    while(--mark > 0) {
        forEachDir(x, y, (px, py) => {
            if (mark == marks.get(px, py)) {
                path.unshift(px, py);
                x = px;
                y = py;
                return false;
            }
        })
    }

    return path;
}

function generateMarks(fromX, fromY, toX, toY, action) {
    const marks = new Marks(fromX, fromY);
    let mark = 0;
    const queue = [fromX, fromY];
    let targetFound = false;

    while (!targetFound && queue[0] != null) {
        const cellX = queue.shift();
        const cellY = queue.shift();
        mark = marks.get(cellX, cellY) + 1;

        forEachDir(cellX, cellY, (x, y) => {
            const type = scheme.get(x, y);
            if (marks.get(x, y) == null && (type == '_' || type == '?') && !isEnemyHere(x, y)) {
                marks.set(x, y, mark);
                queue.push(x, y);

                if (action != null) {
                    action(cellX, cellY, x, y);
                }

                if (x == toX && y == toY) {
                    targetFound = true;
                    return false;
                }
            }
        });
    }

    return marks;
}

function isEnemyHere(x, y) {
    for (let i = 0; i < n - 1; i++) {
        const enemy = coords[i];
        if (enemy.x == x && enemy.y == y) 
            return true;
    }

    return false;
}

function forEachDir(x, y, action) {
    for (let d of dirs) {
        let x1 = (x + d.x + width) % width;
        let y1 = (y + d.y + height) % height;
        if (action(x1, y1, d) === false) 
            return;
    }
}

function Marks(x, y) {
    const _map = {};
    this.x = x;
    this.y = y;
    this.map = _map;
    _map[key(x, y)] = 0;

    this.get = function(x, y) {
        return _map[key(x, y)];
    };

    this.set = function(x, y, mark) {
        _map[key(x, y)] = mark;
    };

    function key(x, y) {
        return x + ':' + y;
    }
}