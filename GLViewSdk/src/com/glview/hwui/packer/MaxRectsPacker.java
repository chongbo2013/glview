package com.glview.hwui.packer;

import java.util.List;
import java.util.Vector;

import com.glview.graphics.Rect;

/**
 * 纹理打包算法
 * @author lijing.lj
 */
public class MaxRectsPacker implements Packer {

	int mWidth, mHeight;
	boolean mAllowRotation;
	
	Vector<PackerRect> mUsedRects = new Vector<PackerRect>();
	Vector<Rect> mFreeRects = new Vector<Rect>();

	Score mScore = new Score();
	
	FreeRectangleChoiceHeuristic mDefaultMethod;
	
	class Score {
		int score1 = 0; // Unused in this function. We don't need to know
		// the score after finding the position.
		int score2 = 0;
		int bestShortSideFit;
		int bestLongSideFit;
	}
	
	public MaxRectsPacker(int width, int height) {
		this(width, height, false);
	}

	public MaxRectsPacker(int width, int height, boolean rotation) {
		this(width, height, rotation, FreeRectangleChoiceHeuristic.BestShortSideFit);
	}
	
	public MaxRectsPacker(int width, int height, boolean rotation, FreeRectangleChoiceHeuristic method) {
		mDefaultMethod = method;
		init(width, height, rotation);
	}

	private void init(int width, int height, boolean rotation) {
		if (!isPowerOf2(width) || !isPowerOf2(height))
			throw new RuntimeException("Must be 2,4,8,16,32,...512,1024,...");
		mWidth = width;
		mHeight = height;
		mAllowRotation = rotation;

		Rect root = new Rect(0, 0, width, height);
		mUsedRects.clear();
		mFreeRects.clear();
		mFreeRects.add(root);
	}
	
	public void reset() {
		Rect root = new Rect(0, 0, mWidth, mHeight);
		mUsedRects.clear();
		mFreeRects.clear();
		mFreeRects.add(root);
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("us=" + mUsedRects.size());
		sb.append("fs=" + mFreeRects.size());
		sb.append("usedRect=");
		sb.append(mUsedRects);
		sb.append(", freeRect=");
		sb.append(mFreeRects);
		return sb.toString();
	}

	private boolean isPowerOf2(int n) {
		if (n == 2) return true;
		if (n < 2 || n % 2 != 0) return false;
		return isPowerOf2(n / 2);
	}
	
	public PackerRect insert(int width, int height) {
		return insert(width, height, mDefaultMethod);
	}

	/**
	 * Insert a new Rectangle
	 * 
	 * @param width
	 * @param height
	 * @param method
	 * @return
	 * 
	 */
	public PackerRect insert(int width, int height,
			FreeRectangleChoiceHeuristic method) {
		PackerRect newNode = null;
		switch (method) {
		case BestShortSideFit:
			newNode = findPositionForNewNodeBestShortSideFit(width, height, mScore);
			break;
		case BottomLeftRule:
			newNode = findPositionForNewNodeBottomLeft(width, height/*, score1,
					score2*/, mScore);
			break;
		case ContactPointRule:
			newNode = findPositionForNewNodeContactPoint(width, height/*, score1*/, mScore);
			break;
		case BestLongSideFit:
			newNode = findPositionForNewNodeBestLongSideFit(width, height/*,
					score2, score1*/, mScore);
			break;
		case BestAreaFit:
			newNode = findPositionForNewNodeBestAreaFit(width, height/*, score1,
					score2*/, mScore);
			break;
		default:
			break;
		}
		if (newNode == null || newNode.height() == 0)
			return null;
		placeRectangle(newNode);
		return newNode;
	}

	public List<Rect> insert2(List<Rect> rects, FreeRectangleChoiceHeuristic method) {
		while (rects.size() > 0) {
			int bestScore1 = Integer.MAX_VALUE;
			int bestScore2 = Integer.MAX_VALUE;
			int bestRectangleIndex = -1;
			PackerRect bestNode = new PackerRect();
			for (int i = 0; i < rects.size(); ++i) {
				mScore.score1 = 0;
				mScore.score2 = 0;
				PackerRect newNode = scoreRectangle(rects.get(i).width(), rects
						.get(i).height(), method, mScore);
				if (newNode != null && (mScore.score1 < bestScore1
						|| (mScore.score1 == bestScore1 && mScore.score2 < bestScore2))) {
					bestScore1 = mScore.score1;
					bestScore2 = mScore.score2;
					bestNode = newNode;
					bestRectangleIndex = i;
				}
			}
			if (bestRectangleIndex == -1)
				return rects;
			placeRectangle(bestNode);
			rects.remove(bestRectangleIndex);
		}
		return rects;
	}

	private PackerRect scoreRectangle(int width, int height,
			FreeRectangleChoiceHeuristic method, Score score) {
		PackerRect newNode = null;
		score.score1 = Integer.MAX_VALUE;
		score.score2 = Integer.MAX_VALUE;
		switch (method) {
		case BestShortSideFit:
			newNode = findPositionForNewNodeBestShortSideFit(width, height, score);
			break;
		case BottomLeftRule:
			newNode = findPositionForNewNodeBottomLeft(width, height/*, score1,
					score2*/, score);
			break;
		case ContactPointRule:
			newNode = findPositionForNewNodeContactPoint(width, height/*, score1*/, score);
			// todo: reverse
			score.score1 = - score.score1; // Reverse since we are minimizing, but for
								// contact point score bigger is better.
			break;
		case BestLongSideFit:
			newNode = findPositionForNewNodeBestLongSideFit(width, height,
					/*score2, score1*/score);
			break;
		case BestAreaFit:
			newNode = findPositionForNewNodeBestAreaFit(width, height, /*score1,
					score2*/score);
			break;
		}

		// Cannot fit the current Rectangle.
		if (newNode == null || newNode.height() == 0) {
			score.score1 = Integer.MAX_VALUE;
			score.score2 = Integer.MAX_VALUE;
			return null;
		}
		return newNode;
	}
	
	private PackerRect bestRect(PackerRect bestNode, int x, int y, int width, int height, boolean rotation) {
		bestNode.set(x, y, x + width, y + height);
		bestNode.rotation = rotation;
		return bestNode;
	}

	private PackerRect findPositionForNewNodeBestShortSideFit(int width, int height, Score iscore) {
		PackerRect bestNode = new PackerRect();
		// memset(&bestNode, 0, sizeof(Rectangle));

		iscore.bestShortSideFit = Integer.MAX_VALUE;
		iscore.bestLongSideFit = iscore.score2;
		Rect rect;
		int leftoverHoriz;
		int leftoverVert;
		int shortSideFit;
		int longSideFit;

		for (int i = 0; i < mFreeRects.size(); i++) {
			rect = mFreeRects.get(i);
			// Try to place the Rectangle in upright (non-flipped) orientation.
			if (rect.width() >= width && rect.height() >= height) {
				leftoverHoriz = Math.abs(rect.width() - width);
				leftoverVert = Math.abs(rect.height() - height);
				shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (shortSideFit < iscore.bestShortSideFit
						|| (shortSideFit == iscore.bestShortSideFit && longSideFit < iscore.bestLongSideFit)) {
					bestRect(bestNode, rect.left, rect.top, width, height, false);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + width;
					bestNode.bottom = bestNode.top + height;*/
					iscore.bestShortSideFit = shortSideFit;
					iscore.bestLongSideFit = longSideFit;
				}
			}
			int flippedLeftoverHoriz;
			int flippedLeftoverVert;
			int flippedShortSideFit;
			int flippedLongSideFit;
			if (mAllowRotation && rect.width() >= height
					&& rect.height() >= width) {
				flippedLeftoverHoriz = Math.abs(rect.width() - height);
				flippedLeftoverVert = Math.abs(rect.height() - width);
				flippedShortSideFit = Math.min(flippedLeftoverHoriz,
						flippedLeftoverVert);
				flippedLongSideFit = Math.max(flippedLeftoverHoriz,
						flippedLeftoverVert);

				if (flippedShortSideFit < iscore.bestShortSideFit
						|| (flippedShortSideFit == iscore.bestShortSideFit && flippedLongSideFit < iscore.bestLongSideFit)) {
					bestRect(bestNode, rect.left, rect.top, height, width, true);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.top + height;
					bestNode.bottom = bestNode.left + width;*/
					iscore.bestShortSideFit = flippedShortSideFit;
					iscore.bestLongSideFit = flippedLongSideFit;
				}
			}
		}
		return bestNode;
	}

	private PackerRect findPositionForNewNodeBottomLeft(int width, int height/*,
			int bestY, int bestX*/, Score iscore) {
		PackerRect bestNode = new PackerRect();
		// memset(bestNode, 0, sizeof(Rectangle));
		
		iscore.score1 = Integer.MAX_VALUE;
		Rect rect;
		int topSideY;
		for (int i = 0; i < mFreeRects.size(); i++) {
			rect = mFreeRects.get(i);
			// Try to place the Rectangle in upright (non-flipped) orientation.
			if (rect.width() >= width && rect.height() >= height) {
				topSideY = rect.top + height;
				if (topSideY < iscore.score1
						|| (topSideY == iscore.score1 && rect.left < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, width, height, false);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + width;
					bestNode.bottom = bestNode.top + height;*/
					iscore.score1 = topSideY;
					iscore.score2 = rect.left;
				}
			}
			if (mAllowRotation && rect.width() >= height
					&& rect.height() >= width) {
				topSideY = rect.top + width;
				if (topSideY < iscore.score1
						|| (topSideY == iscore.score1 && rect.left < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, height, width, true);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + height;
					bestNode.bottom = bestNode.top + width;*/
					iscore.score1 = topSideY;
					iscore.score2 = rect.left;
				}
			}
		}
		return bestNode;
	}

	// / Returns 0 if the two intervals i1 and i2 are disjoint, or the length of
	// their overlap otherwise.
	private int commonIntervalLength(int i1start, int i1end, int i2start,
			int i2end) {
		if (i1end < i2start || i2end < i1start)
			return 0;
		return Math.min(i1end, i2end) - Math.max(i1start, i2start);
	}

	private int contactPointScoreNode(int x, int y, int width, int height) {
		int score = 0;

		if (x == 0 || x + width == mWidth)
			score += height;
		if (y == 0 || y + height == mHeight)
			score += width;
		Rect rect;
		for (int i = 0; i < mUsedRects.size(); i++) {
			rect = mUsedRects.get(i).rect();
			if (rect.left == x + width || rect.right == x)
				score += commonIntervalLength(rect.top, rect.bottom, y, y
						+ height);
			if (rect.top == y + height || rect.bottom == y)
				score += commonIntervalLength(rect.left, rect.right, x, x
						+ width);
		}
		return score;
	}

	private PackerRect findPositionForNewNodeContactPoint(int width, int height/*,
			int bestContactScore*/, Score iscore) {
		PackerRect bestNode = new PackerRect();
		// memset(&bestNode, 0, sizeof(Rectangle));

		iscore.score1 = -1;

		Rect rect;
		int score;
		for (int i = 0; i < mFreeRects.size(); i++) {
			rect = mFreeRects.get(i);
			// Try to place the Rectangle in upright (non-flipped) orientation.
			if (rect.width() >= width && rect.height() >= height) {
				score = contactPointScoreNode(rect.left, rect.top, width,
						height);
				if (score > iscore.score1) {
					bestRect(bestNode, rect.left, rect.top, width, height, false);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + width;
					bestNode.bottom = bestNode.top + height;*/
					iscore.score1 = score;
				}
			}
			if (mAllowRotation && rect.width() >= height
					&& rect.height() >= width) {
				score = contactPointScoreNode(rect.left, rect.top, height,
						width);
				if (score > iscore.score1) {
					bestRect(bestNode, rect.left, rect.top, height, width, true);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + height;
					bestNode.bottom = bestNode.top + width;*/
					iscore.score1 = score;
				}
			}
		}
		return bestNode;
	}

	private PackerRect findPositionForNewNodeBestLongSideFit(int width, int height/*,
			int bestShortSideFit, int bestLongSideFit*/, Score iscore) {
		PackerRect bestNode = new PackerRect();
		// memset(&bestNode, 0, sizeof(Rectangle));
		iscore.score1 = Integer.MAX_VALUE;
		Rect rect;

		int leftoverHoriz;
		int leftoverVert;
		int shortSideFit;
		int longSideFit;
		for (int i = 0; i < mFreeRects.size(); i++) {
			rect = mFreeRects.get(i);
			// Try to place the Rectangle in upright (non-flipped) orientation.
			if (rect.width() >= width && rect.height() >= height) {
				leftoverHoriz = Math.abs(rect.width() - width);
				leftoverVert = Math.abs(rect.height() - height);
				shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (longSideFit < iscore.score1
						|| (longSideFit == iscore.score1 && shortSideFit < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, width, height, false);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + width;
					bestNode.bottom = bestNode.top + height;*/
					iscore.score2 = shortSideFit;
					iscore.score1 = longSideFit;
				}
			}

			if (mAllowRotation && rect.width() >= height
					&& rect.height() >= width) {
				leftoverHoriz = Math.abs(rect.width() - height);
				leftoverVert = Math.abs(rect.height() - width);
				shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (longSideFit < iscore.score1
						|| (longSideFit == iscore.score1 && shortSideFit < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, height, width, true);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + height;
					bestNode.bottom = bestNode.top + width;*/
					iscore.score2 = shortSideFit;
					iscore.score1 = longSideFit;
				}
			}
		}
		return bestNode;
	}

	private PackerRect findPositionForNewNodeBestAreaFit(int width, int height/*,
			int bestAreaFit, int bestShortSideFit*/, Score iscore) {
		PackerRect bestNode = new PackerRect();
		// memset(&bestNode, 0, sizeof(Rectangle));

//		bestAreaFit = Integer.MAX_VALUE;
		iscore.score1 = Integer.MAX_VALUE;

		Rect rect;

		int leftoverHoriz;
		int leftoverVert;
		int shortSideFit;
		int areaFit;

		for (int i = 0; i < mFreeRects.size(); i++) {
			rect = mFreeRects.get(i);
			areaFit = rect.width() * rect.height() - width * height;

			// Try to place the Rectangle in upright (non-flipped) orientation.
			if (rect.width() >= width && rect.height() >= height) {
				leftoverHoriz = Math.abs(rect.width() - width);
				leftoverVert = Math.abs(rect.height() - height);
				shortSideFit = Math.min(leftoverHoriz, leftoverVert);

				if (areaFit < iscore.score1
						|| (areaFit == iscore.score1 && shortSideFit < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, width, height, false);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + width;
					bestNode.bottom = bestNode.top + height;*/
					iscore.score2 = shortSideFit;
					iscore.score1 = areaFit;
				}
			}

			if (mAllowRotation && rect.width() >= height
					&& rect.height() >= width) {
				leftoverHoriz = Math.abs(rect.width() - height);
				leftoverVert = Math.abs(rect.height() - width);
				shortSideFit = Math.min(leftoverHoriz, leftoverVert);

				if (areaFit < iscore.score1
						|| (areaFit == iscore.score1 && shortSideFit < iscore.score2)) {
					bestRect(bestNode, rect.left, rect.top, height, width, true);
					/*bestNode.left = rect.left;
					bestNode.top = rect.top;
					bestNode.right = bestNode.left + height;
					bestNode.bottom = bestNode.top + width;*/
					iscore.score2 = shortSideFit;
					iscore.score1 = areaFit;
				}
			}
		}
		return bestNode;
	}

	private void placeRectangle(PackerRect node) {
		int numRectanglesToProcess = mFreeRects.size();
		for (int i = 0; i < numRectanglesToProcess; i++) {
			if (splitFreeNode(mFreeRects.get(i), node.rect())) {
				mFreeRects.remove(i);
				--i;
				--numRectanglesToProcess;
			}
		}
		pruneFreeList();
		mUsedRects.add(node);
	}

	private boolean splitFreeNode(Rect freeNode, Rect usedNode) {
		// Test with SAT if the Rectangles even intersect.
		if (usedNode.left >= freeNode.right || usedNode.right <= freeNode.left
				|| usedNode.top >= freeNode.bottom
				|| usedNode.bottom <= freeNode.top)
			return false;
		Rect newNode;
		if (usedNode.left < freeNode.right && usedNode.right > freeNode.left) {
			// New node at the top side of the used node.
			if (usedNode.top > freeNode.top && usedNode.top < freeNode.bottom) {
				newNode = new Rect(freeNode);
				newNode.bottom = newNode.top + (usedNode.top - newNode.top);
				mFreeRects.add(newNode);
			}

			// New node at the bottom side of the used node.
			if (usedNode.bottom < freeNode.bottom) {
				newNode = new Rect(freeNode);
				newNode.top = usedNode.bottom;
				newNode.bottom = newNode.top
						+ (freeNode.bottom - usedNode.bottom);
				mFreeRects.add(newNode);
			}
		}

		if (usedNode.top < freeNode.bottom && usedNode.bottom > freeNode.top) {
			// New node at the left side of the used node.
			if (usedNode.left > freeNode.left && usedNode.left < freeNode.right) {
				newNode = new Rect(freeNode);
				newNode.right = newNode.left + (usedNode.left - newNode.left);
				mFreeRects.add(newNode);
			}

			// New node at the right side of the used node.
			if (usedNode.right < freeNode.right) {
				newNode = new Rect(freeNode);
				newNode.left = usedNode.right;
				newNode.right = newNode.left
						+ (freeNode.right - usedNode.right);
				mFreeRects.add(newNode);
			}
		}
		return true;
	}

	private void pruneFreeList() {
		int numRectanglesToProcess = mFreeRects.size();
		for (int i = 0; i < numRectanglesToProcess; i++) {
			for (int j = i + 1; j < numRectanglesToProcess; j ++) {
				if (isContainedIn(mFreeRects.get(i), mFreeRects.get(j))) {
					mFreeRects.remove(i);
					--i;
					--numRectanglesToProcess;
					break;
				}
				if (isContainedIn(mFreeRects.get(j), mFreeRects.get(i))) {
					mFreeRects.remove(j);
					--j;
					--numRectanglesToProcess;
				}
			}
		}
	}

	private boolean isContainedIn(Rect a, Rect b) {
		return b.contains(a);
	}

	public static enum FreeRectangleChoiceHeuristic {
		BestShortSideFit, BottomLeftRule, ContactPointRule, BestLongSideFit, BestAreaFit
	}
}
