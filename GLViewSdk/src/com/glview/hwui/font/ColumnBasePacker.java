package com.glview.hwui.font;

import com.glview.hwui.packer.Packer;
import com.glview.hwui.packer.PackerRect;

public class ColumnBasePacker implements Packer {

	private final static int CACHE_BLOCK_ROUNDING_SIZE = 4;
	
	int mWidth, mHeight;
	
	CacheBlock mCacheBlocks;
	
	public ColumnBasePacker(int width, int height) {
		mWidth = width;
		mHeight = height;
		mCacheBlocks = new CacheBlock(0, 0,
	            mWidth, mHeight);
	}
	
	@Override
	public PackerRect insert(int width, int height) {
		if (height > mHeight) {
	        return null;
	    }
		// roundedUpW equals glyphW to the next multiple of CACHE_BLOCK_ROUNDING_SIZE.
	    // This columns for glyphs that are close but not necessarily exactly the same size. It trades
	    // off the loss of a few pixels for some glyphs against the ability to store more glyphs
	    // of varying sizes in one block.
	    int roundedUpW = (width + CACHE_BLOCK_ROUNDING_SIZE - 1) & -CACHE_BLOCK_ROUNDING_SIZE;
	    CacheBlock cacheBlock = mCacheBlocks;
	    int retOriginX = 0, retOriginY = 0;
	    while (cacheBlock != null) {
	        // Store glyph in this block iff: it fits the block's remaining space and:
	        // it's the remainder space (mY == 0) or there's only enough height for this one glyph
	        // or it's within ROUNDING_SIZE of the block width
	        if (roundedUpW <= cacheBlock.mWidth && height <= cacheBlock.mHeight &&
	                (cacheBlock.mY == 0 ||
	                        (cacheBlock.mWidth - roundedUpW < CACHE_BLOCK_ROUNDING_SIZE))) {
	            if (cacheBlock.mHeight - height < height) {
	                // Only enough space for this glyph - don't bother rounding up the width
	                roundedUpW = width;
	            }

	            retOriginX = cacheBlock.mX;
	            retOriginY = cacheBlock.mY;

	            // If this is the remainder space, create a new cache block for this column. Otherwise,
	            // adjust the info about this column.
	            if (cacheBlock.mY == 0) {
	                int oldX = cacheBlock.mX;
	                // Adjust remainder space dimensions
	                cacheBlock.mWidth -= roundedUpW;
	                cacheBlock.mX += roundedUpW;

	                if (mHeight - height >= height) {
	                    // There's enough height left over to create a new CacheBlock
	                    CacheBlock newBlock = new CacheBlock(oldX, height,
	                            roundedUpW, mHeight - height);
	                    mCacheBlocks = CacheBlock.insertBlock(mCacheBlocks, newBlock);
	                }
	            } else {
	                // Insert into current column and adjust column dimensions
	                cacheBlock.mY += height;
	                cacheBlock.mHeight -= height;
	            }

	            if (cacheBlock.mHeight < Math.min(height, width)) {
	                // If remaining space in this block is too small to be useful, remove it
	                mCacheBlocks = CacheBlock.removeBlock(mCacheBlocks, cacheBlock);
	            }

	            return new PackerRect(retOriginX, retOriginY, retOriginX + width, retOriginY + height);
	        }
	        cacheBlock = cacheBlock.mNext;
	    }
	    return null;
	}

	@Override
	public void reset() {
		mCacheBlocks = new CacheBlock(0, 0,
	            mWidth, mHeight);
	}

	@Override
	public String dump() {
		StringBuffer sb = new StringBuffer();
		CacheBlock currBlock = mCacheBlocks;
        while (currBlock != null) {
            sb.append("Block x=");
            sb.append(currBlock.mX);
            sb.append(" y=");
            sb.append(currBlock.mY);
            sb.append(" w=");
            sb.append(currBlock.mWidth);
            sb.append(" h=");
            sb.append(currBlock.mHeight);
            sb.append("\n");
            currBlock = currBlock.mNext;
        }
		return sb.toString();
	}
	
	static class CacheBlock {
		int mX;
	    int mY;
	    int mWidth;
	    int mHeight;
	    CacheBlock mNext;
	    CacheBlock mPrev;
	    
	    public CacheBlock(int x, int y, int width, int height) {
	    	mX = x;
	    	mY = y;
	    	mWidth = width;
	    	mHeight = height;
		}
	    
	    static CacheBlock insertBlock(CacheBlock head, CacheBlock newBlock) {
	    	CacheBlock currBlock = head;
	        CacheBlock prevBlock = null;

	        while (currBlock != null && currBlock.mY != 0) {
	            if (newBlock.mWidth < currBlock.mWidth) {
	                newBlock.mNext = currBlock;
	                newBlock.mPrev = prevBlock;
	                currBlock.mPrev = newBlock;

	                if (prevBlock != null) {
	                    prevBlock.mNext = newBlock;
	                    return head;
	                } else {
	                    return newBlock;
	                }
	            }

	            prevBlock = currBlock;
	            currBlock = currBlock.mNext;
	        }

	        // new block larger than all others - insert at end (but before the remainder space, if there)
	        newBlock.mNext = currBlock;
	        newBlock.mPrev = prevBlock;

	        if (currBlock != null) {
	            currBlock.mPrev = newBlock;
	        }

	        if (prevBlock != null) {
	            prevBlock.mNext = newBlock;
	            return head;
	        } else {
	            return newBlock;
	        }
	    }
	    static CacheBlock removeBlock(CacheBlock head, CacheBlock blockToRemove) {
	    	CacheBlock newHead = head;
	        CacheBlock nextBlock = blockToRemove.mNext;
	        CacheBlock prevBlock = blockToRemove.mPrev;

	        if (prevBlock != null) {
	            prevBlock.mNext = nextBlock;
	        } else {
	            newHead = nextBlock;
	        }

	        if (nextBlock != null) {
	            nextBlock.mPrev = prevBlock;
	        }

	        return newHead;
	    }
	}

}
