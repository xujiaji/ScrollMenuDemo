package com.example.jiana.scrollmenudemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvVertical, rvHorizontal;
    private ScrollView scrollView;
    private List<String> datas;
    private ScrollMenu scrollMenu;
    private MyAdapter adapter;
    private CheckedTextView ctvH, ctvV;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        inflateData();
        addListener();
    }


    private void init() {
        scrollMenu = (ScrollMenu) findViewById(R.id.scrollMenu);
        rvVertical = (RecyclerView) findViewById(R.id.rvVertical);
        rvHorizontal = (RecyclerView) findViewById(R.id.rvHorizontal);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        ctvH = (CheckedTextView) findViewById(R.id.ctvH);
        ctvV = (CheckedTextView) findViewById(R.id.ctvV);
        rvVertical.setLayoutManager(new LinearLayoutManager(this));
        rvHorizontal.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void inflateData() {
        datas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            datas.add(index + "-item : " + i);
        }
        adapter = new MyAdapter(datas);
        rvHorizontal.setAdapter(adapter);
        rvVertical.setAdapter(adapter);
    }

    private int index = 0;
    private void changeData(boolean isTop) {
        datas.clear();
        index += isTop ? -1 : 1;
        for (int i = 0; i < 20; i++) {
            datas.add(index + "-tem : " + i);
        }
        adapter.notifyDataSetChanged();
    }

    private void addListener() {
        scrollMenu.setOnScrollCompleteListener(new ScrollMenu.OnScrollCompleteListener() {
            @Override
            public void completeTop() {
                Toast.makeText(MainActivity.this, "↑↑上滑切换↑↑", Toast.LENGTH_SHORT).show();
                changeData(true);
            }

            @Override
            public void completeBottom() {
                Toast.makeText(MainActivity.this, "↓↓下滑切换↓↓", Toast.LENGTH_SHORT).show();
                changeData(false);
            }
        });

        adapter.setOnItemClick(new MyAdapter.OnItemClick() {
            @Override
            public void onPos(int position) {
                Toast.makeText(MainActivity.this, "click item = " + position, Toast.LENGTH_SHORT).show();
            }
        });

        ctvH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctvH.toggle();
                ctvH.setText(ctvH.isChecked() ? "横向滑动开" : "横向滑动关");
                scrollMenu.setOpenHorizontalSlide(ctvH.isChecked());
            }
        });

        ctvV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctvV.toggle();
                ctvV.setText(ctvV.isChecked() ? "纵向滑动开" : "纵向滑动关");
                scrollMenu.setOpenVerticalSlide(ctvV.isChecked());
            }
        });
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyHolder> {
        private List<String> datas;
        public MyAdapter(List<String> datas) {
            this.datas = datas;
        }
        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(MyHolder holder, final int position) {
            holder.tv.setText(datas.get(position));
            holder.tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClick != null) {
                        onItemClick.onPos(position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        public static class MyHolder extends RecyclerView.ViewHolder {
            TextView tv;
            public MyHolder(View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }

        public interface OnItemClick {
            void onPos(int position);
        }

        private OnItemClick onItemClick;
        public void setOnItemClick(OnItemClick onItemClick) {
            this.onItemClick = onItemClick;
        }
    }

}
