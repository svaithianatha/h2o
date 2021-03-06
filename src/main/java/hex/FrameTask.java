package hex;

import water.H2O.H2OCountedCompleter;
import water.*;
import water.Job.JobCancelledException;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;
import water.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class FrameTask<T extends FrameTask<T>> extends MRTask2<T>{
  final protected DataInfo _dinfo;
  final Job _job;
  double    _ymu = Double.NaN; // mean of the response
  // size of the expanded vector of parameters

  protected float _useFraction = 1.0f;
  protected long _seed;
  protected boolean _shuffle = false;

  public FrameTask(Job job, DataInfo dinfo) {
    this(job,dinfo,null);
  }
  public FrameTask(Job job, DataInfo dinfo, H2OCountedCompleter cmp) {
    super(cmp);
    _job = job;
    _dinfo = dinfo;
    _seed = new Random().nextLong();
  }
  protected FrameTask(FrameTask ft){
    _dinfo = ft._dinfo;
    _job = ft._job;
    _useFraction = ft._useFraction;
    _seed = ft._seed;
    _shuffle = ft._shuffle;
  }
  public double [] normMul(){return _dinfo._normMul;}
  public double [] normSub(){return _dinfo._normSub;}

  /**
   * Method to process one row of the data for GLM functions.
   * Numeric and categorical values are passed separately, as is response.
   * Categoricals are passed as absolute indexes into the expanded beta vector, 0-levels are skipped
   * (so the number of passed categoricals will not be the same for every row).
   *
   * Categorical expansion/indexing:
   *   Categoricals are placed in the beginning of the beta vector.
   *   Each cat variable with n levels is expanded into n-1 independent binary variables.
   *   Indexes in cats[] will point to the appropriate coefficient in the beta vector, so e.g.
   *   assume we have 2 categorical columns both with values A,B,C, then the following rows will have following indexes:
   *      A,A - ncats = 0, we do not pass any categorical here
   *      A,B - ncats = 1, indexes = [2]
   *      B,B - ncats = 2, indexes = [0,2]
   *      and so on
   *
   * @param gid      - global id of this row, in [0,_adaptedFrame.numRows())
   * @param nums     - numeric values of this row
   * @param ncats    - number of passed (non-zero) categoricals
   * @param cats     - indexes of categoricals into the expanded beta-vector.
   * @param response - numeric value for the response
   */
  protected void processRow(long gid, double [] nums, int ncats, int [] cats, double [] response){throw new RuntimeException("should've been overriden!");}
  protected void processRow(long gid, double [] nums, int ncats, int [] cats, double [] response, NewChunk [] outputs){throw new RuntimeException("should've been overriden!");}


  public static class DataInfo extends Iced {
    public Frame _adaptedFrame;
    public final int _responses; // number of responses
    public final boolean _standardize;
    public final int _nums;
    public final int _cats;
    public final int [] _catOffsets;
    public final double [] _normMul;
    public final double [] _normSub;
    public final int _foldId;
    public final int _nfolds;

    private DataInfo(DataInfo dinfo, int foldId, int nfolds){
      _standardize = dinfo._standardize;
      _responses = dinfo._responses;
      _nums = dinfo._nums;
      _cats = dinfo._cats;
      _adaptedFrame = dinfo._adaptedFrame;
      _catOffsets = dinfo._catOffsets;
      _normMul = dinfo._normMul;
      _normSub = dinfo._normSub;
      _foldId = foldId;
      _nfolds = nfolds;
    }
    public DataInfo(Frame fr, int hasResponses, double [] normSub, double [] normMul){
      this(fr,hasResponses,normSub != null && normMul != null);
      assert (normSub == null) == (normMul == null);
      if(normSub != null && normMul != null){
        System.arraycopy(normSub, 0, _normSub, 0, normSub.length);
        System.arraycopy(normMul, 0, _normMul, 0, normMul.length);
      }
    }

    /**
     * Prepare a Frame (with a single response) to be processed by the FrameTask
     * 1) Place response at the end
     * 2) (Optionally) Remove rows with constant values or with >20% NaNs
     * 3) Possibly turn integer categoricals into enums
     *
     * @param source A frame to be expanded and sanity checked
     * @param response (should be part of source)
     * @param toEnum Whether or not to turn categoricals into enums
     * @param dropConstantCols Whether or not to drop constant columns
     * @return Frame to be used by FrameTask
     */
    public static Frame prepareFrame(Frame source, Vec response, int[] ignored_cols, boolean toEnum, boolean dropConstantCols) {
      Frame fr = new Frame(source._names.clone(), source.vecs().clone());
      if (ignored_cols != null) fr.remove(ignored_cols);
      final Vec[] vecs =  fr.vecs();
      // put response to the end (if not already)
      for(int i = 0; i < vecs.length-1; ++i) {
        if(vecs[i] == response){
          final String n = fr._names[i];
          if (toEnum && !vecs[i].isEnum()) fr.add(n, fr.remove(i).toEnum()); //convert int classes to enums
          else fr.add(n, fr.remove(i));
          break;
        }
      }
      // special case for when response was at the end already
      if (toEnum && !response.isEnum() && vecs[vecs.length-1] == response) {
        final String n = fr._names[vecs.length-1];
        fr.add(n, fr.remove(vecs.length-1).toEnum());
      }
      ArrayList<Integer> constantOrNAs = new ArrayList<Integer>();
      for(int i = 0; i < vecs.length-1; ++i) {
        // remove constant cols and cols with too many NAs
        if( (dropConstantCols && vecs[i].min() == vecs[i].max()) || vecs[i].naCnt() > vecs[i].length()*0.2)
          constantOrNAs.add(i);
      }
      if(!constantOrNAs.isEmpty()){
        int [] cols = new int[constantOrNAs.size()];
        for(int i = 0; i < cols.length; ++i)
          cols[i] = constantOrNAs.get(i);
        fr.remove(cols);
      }
      return fr;
    }

    public DataInfo(Frame fr, int nResponses, boolean standardize){
      _nfolds = _foldId = 0;
      _standardize = standardize;
      _responses = nResponses;
      final Vec [] vecs = fr.vecs();
      final int n = vecs.length-_responses;
      int [] nums = MemoryManager.malloc4(n);
      int [] cats = MemoryManager.malloc4(n);
      int nnums = 0, ncats = 0;
      for(int i = 0; i < n; ++i){
        if(vecs[i].isEnum())
          cats[ncats++] = i;
        else
          nums[nnums++] = i;
      }
      _nums = nnums;
      _cats = ncats;
      // sort the cats in the decreasing order according to their size
      for(int i = 0; i < ncats; ++i)
        for(int j = i+1; j < ncats; ++j)
          if(vecs[cats[i]].domain().length < vecs[cats[j]].domain().length){
            int x = cats[i];
            cats[i] = cats[j];
            cats[j] = x;
          }
      Vec [] vecs2 = vecs.clone();
      String [] names = fr._names.clone();
      _catOffsets = MemoryManager.malloc4(ncats+1);
      int len = _catOffsets[0] = 0;

      for(int i = 0; i < ncats; ++i){
        Vec v = (vecs2[i] = vecs[cats[i]]);
        names[i] = fr._names[cats[i]];
        _catOffsets[i+1] = (len += v.domain().length - 1);
      }
      if(standardize){
        _normSub = MemoryManager.malloc8d(nnums);
        _normMul = MemoryManager.malloc8d(nnums); Arrays.fill(_normMul, 1);
      } else
        _normSub = _normMul = null;
      for(int i = 0; i < nnums; ++i){
        Vec v = (vecs2[i+ncats]  = vecs [nums[i]]);
        names[i+ncats] = fr._names[nums[i]];
        if(standardize){
          _normSub[i] = v.mean();
          _normMul[i] = v.sigma() != 0 ? 1.0/v.sigma() : 1.0;
        }
      }
      _adaptedFrame = new Frame(names,vecs2);
    }
    public String toString(){
      return "";
    }
    public DataInfo getFold(int foldId, int nfolds){
      return new DataInfo(this, foldId, nfolds);
    }
    public final int fullN(){return _nums + _catOffsets[_cats];}
    public final int largestCat(){return _cats > 0?_catOffsets[1]:0;}
    public final int numStart(){return _catOffsets[_cats];}
    public final String [] coefNames(){
      int k = 0;
      final int n = fullN();
      String [] res = new String[n];
      final Vec [] vecs = _adaptedFrame.vecs();
      for(int i = 0; i < _cats; ++i)
        for(int j = 1; j < vecs[i]._domain.length; ++j)
          res[k++] = _adaptedFrame._names[i] + "." + vecs[i]._domain[j];
      final int nums = n-k;
      for(int i = 0; i < nums; ++i)
        res[k+i] = _adaptedFrame._names[_cats+i];
      return res;
    }
  }

  @Override
  public T dfork(Frame fr){
    assert fr == _dinfo._adaptedFrame;
    return super.dfork(fr);
  }

  /**
   * Override this to initialize at the beginning of chunk processing.
   */
  protected void chunkInit(){}
  /**
   * Override this to do post-chunk processing work.
   */
  protected void chunkDone(){}


  /**
   * Extracts the values, applies regularization to numerics, adds appropriate offsets to categoricals,
   * and adapts response according to the CaseMode/CaseValue if set.
   */
  @Override public final void map(Chunk [] chunks, NewChunk [] outputs){
    if(_job != null && _job.self() != null && !Job.isRunning(_job.self()))throw new JobCancelledException();
    final int nrows = chunks[0]._len;
    final long offset = chunks[0]._start;
    chunkInit();
    double [] nums = MemoryManager.malloc8d(_dinfo._nums);
    int    [] cats = MemoryManager.malloc4(_dinfo._cats);
    double [] response = MemoryManager.malloc8d(_dinfo._responses);
    int start = 0;
    int end = nrows;

    boolean contiguous = false;
    Random skip_rng = null; //random generator for skipping rows
    if (_useFraction < 1.0) {
      skip_rng = water.util.Utils.getDeterRNG(_seed+++0x600D5EED+offset); //change seed to avoid periodicity across epochs
      if (contiguous) {
        final int howmany = (int)Math.ceil(_useFraction*nrows);
        if (howmany > 0) {
          start = skip_rng.nextInt(nrows - howmany);
          end = start + howmany;
        }
        assert(start < nrows);
        assert(end <= nrows);
      }
    }

    long[] shuf_map = null;
    if (_shuffle) {
      shuf_map = new long[end-start];
      for (int i=0;i<shuf_map.length;++i)
        shuf_map[i] = start + i;
      Utils.shuffleArray(shuf_map, _seed+++0xD0650F1E+offset);
    }

    OUTER:
    for(int rr = start; rr < end; ++rr){
      final int r = shuf_map != null ? (int)shuf_map[rr-start] : rr;
      if ((_dinfo._nfolds > 0 && (r % _dinfo._nfolds) == _dinfo._foldId)
              || (skip_rng != null && skip_rng.nextFloat() > _useFraction))continue;
      for(Chunk c:chunks)if(c.isNA0(r))continue OUTER; // skip rows with NAs!
      int i = 0, ncats = 0;
      for(; i < _dinfo._cats; ++i){
        int c = (int)chunks[i].at80(r);
        if(c != 0)cats[ncats++] = c + _dinfo._catOffsets[i] - 1;
      }
      final int n = chunks.length-_dinfo._responses;
      for(;i < n;++i){
        double d = chunks[i].at0(r);
        if(_dinfo._normMul != null) d = (d - _dinfo._normSub[i-_dinfo._cats])*_dinfo._normMul[i-_dinfo._cats];
        nums[i-_dinfo._cats] = d;
      }
      for(i = 0; i < _dinfo._responses; ++i)
        response[i] = chunks[chunks.length-_dinfo._responses + i].at0(r);
      if(outputs != null && outputs.length > 0)
        processRow(offset+r, nums, ncats, cats, response, outputs);
      else
        processRow(offset+r, nums, ncats, cats, response);
    }
    chunkDone();
  }
}
