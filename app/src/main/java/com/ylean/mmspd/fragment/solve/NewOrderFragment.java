package com.ylean.mmspd.fragment.solve;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.ylean.mmspd.R;
import com.ylean.mmspd.activity.order.OrderDetailsActivity;
import com.ylean.mmspd.adapter.solve.NewOrderAdapter;
import com.ylean.mmspd.eventbus.EventBusType;
import com.ylean.mmspd.eventbus.EventStatus;
import com.zxdc.utils.library.base.BaseApplication;
import com.zxdc.utils.library.base.BaseFragment;
import com.zxdc.utils.library.bean.BaseBean;
import com.zxdc.utils.library.bean.Courier;
import com.zxdc.utils.library.bean.Order;
import com.zxdc.utils.library.http.HandlerConstant;
import com.zxdc.utils.library.http.HttpMethod;
import com.zxdc.utils.library.util.DialogUtil;
import com.zxdc.utils.library.util.LogUtils;
import com.zxdc.utils.library.util.SPUtil;
import com.zxdc.utils.library.util.ToastUtil;
import com.zxdc.utils.library.view.MyRefreshLayout;
import com.zxdc.utils.library.view.MyRefreshLayoutListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
/**
 * 新订单fragment
 * Created by Administrator on 2019/8/23.
 */
public class NewOrderFragment extends BaseFragment implements MyRefreshLayoutListener {

    @BindView(R.id.listView)
    ListView listView;
    @BindView(R.id.re_list)
    MyRefreshLayout reList;
    Unbinder unbinder;
    private NewOrderAdapter newOrderAdapter;
    //页数
    private int page=1;
    //fragment是否可见
    private boolean isVisibleToUser=false;
    //订单数据集合
    private List<Order.OrderBean> listAll=new ArrayList<>();
    //订单id
    private int orderId;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册eventBus
        EventBus.getDefault().register(this);
        //获取物流公司
        getCourier();
    }


    View view=null;
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.listview, container, false);
        unbinder = ButterKnife.bind(this, view);
        listView.setDivider(null);
        //刷新加载
        reList.setMyRefreshLayoutListener(this);
        newOrderAdapter=new NewOrderAdapter(mActivity,listAll);
        listView.setAdapter(newOrderAdapter);
        //获取订单列表
        getOrderList(HandlerConstant.GET_NEW_ORDER_SUCCESS1);
        return view;
    }


    private Handler handler=new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            DialogUtil.closeProgress();
            switch (msg.what){
                //下刷
                case HandlerConstant.GET_NEW_ORDER_SUCCESS1:
                    reList.refreshComplete();
                    listAll.clear();
                    refresh((Order) msg.obj);
                    break;
                //上拉
                case HandlerConstant.GET_NEW_ORDER_SUCCESS2:
                    reList.loadMoreComplete();
                    refresh((Order) msg.obj);
                    break;
                case HandlerConstant.GET_COURIER_SUCCESS:
                     Courier courier= (Courier) msg.obj;
                     if(courier==null){
                         break;
                     }
                     if(courier.isSussess()){
                        SPUtil.getInstance(mActivity).addObject(SPUtil.COURIER,courier);
                     }
                    break;
                //订单发货
                case HandlerConstant.GET_ORDER_FH_SUCCESS:
                      final BaseBean baseBean= (BaseBean) msg.obj;
                      if(baseBean==null){
                          break;
                      }
                      if(baseBean.isSussess()){
                          for (int i=0;i<listAll.size();i++){
                               if(listAll.get(i).getOrderid()==orderId){
                                   listAll.remove(i);
                                   break;
                               }
                          }
                          newOrderAdapter.maps.clear();
                          newOrderAdapter.fhMap.clear();
                          newOrderAdapter.notifyDataSetChanged();
                          //刷新订单数量
                          EventBus.getDefault().post(new EventBusType(EventStatus.REFRESH_ORDER_NUM));
                      }
                      ToastUtil.showLong(baseBean.getDesc());
                      break;
                case HandlerConstant.REQUST_ERROR:
                    ToastUtil.showLong(msg.obj==null ? "异常错误信息" : msg.obj.toString());
                    break;
                default:
                    break;
            }
            return false;
        }
    });


    /**
     * 刷新界面数据
     */
    private void refresh(Order order){
        if(order==null){
            return;
        }
        if(order.isSussess()){
            List<Order.OrderBean> list=order.getData();
            listAll.addAll(list);
            newOrderAdapter.notifyDataSetChanged();
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    Intent intent=new Intent(mActivity, OrderDetailsActivity.class);
//                    intent.putExtra("code",listAll.get(position).getOrdernum());
//                    startActivity(intent);
                }
            });
            if(list.size()<20){
                reList.setIsLoadingMoreEnabled(false);
            }
        }else{
            ToastUtil.showLong(order.getDesc());
        }
    }


    /**
     * EventBus注解
     */
    @Subscribe
    public void onEvent(EventBusType eventBusType) {
        switch (eventBusType.getStatus()) {
            //订单发货
            case EventStatus.ORDER_STATUS_1:
                  String str=eventBusType.getMsg();
                  if(TextUtils.isEmpty(str)){
                      return;
                  }
                  String[] msg=str.split(",");
                  orderId=Integer.parseInt(msg[0]);
                  fahuo((Courier.CourierBean) eventBusType.getObject(),msg[1]);
                  break;
            default:
                break;
        }
    }


    /**
     * 下刷
     * @param view
     */
    public void onRefresh(View view) {
        page=1;
        HttpMethod.getOrderList(String.valueOf(page),null,"1",HandlerConstant.GET_NEW_ORDER_SUCCESS1,handler);
    }

    /**
     * 上拉加载更多
     * @param view
     */
    public void onLoadMore(View view) {
        page++;
        HttpMethod.getOrderList(String.valueOf(page),null,"1",HandlerConstant.GET_NEW_ORDER_SUCCESS2,handler);
    }



    /**
     * 获取订单列表
     */
    private void getOrderList(int index){
        if(isVisibleToUser && view!=null && listAll.size()==0){
            reList.startRefresh();
        }
    }


    /**
     * 订单发货
     */
    private void fahuo(Courier.CourierBean courierBean,String code){
        DialogUtil.showProgress(mActivity,"发送中...");
        HttpMethod.fahuo(courierBean.getName(),code,courierBean.getCode(),String.valueOf(orderId),handler);
    }

    /**
     * 获取物流公司
     */
    private void getCourier(){
        HttpMethod.getCourier(handler);
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser=isVisibleToUser;
        //获取订单列表
        getOrderList(HandlerConstant.GET_NEW_ORDER_SUCCESS1);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        removeHandler(handler);
        EventBus.getDefault().unregister(this);
    }
}
